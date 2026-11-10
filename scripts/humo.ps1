# ---------------------------------------------------------------
# Prueba de humo de punta a punta CONTRA EL GATEWAY (los 4 servicios deben estar levantados).
#   powershell -ExecutionPolicy Bypass -File scripts/humo.ps1
#   powershell -ExecutionPolicy Bypass -File scripts/humo.ps1 -Gateway http://<ip-ec2>:8080
#   powershell -ExecutionPolicy Bypass -File scripts/humo.ps1 -Salida docs/evidencias/humo.txt
#
# Comprueba: login de los 3 roles, compra sin sesión y con sesión, descuento de stock, 409 por
# stock insuficiente, Idempotency-Key, permisos del Vendedor (403 fuera de lo permitido), 401 sin
# token y que /internal/** no se publica. Crea boletas reales (quedan como evidencia).
# Termina con código 1 si alguna comprobación falla.
# ---------------------------------------------------------------
param(
  [string]$Gateway = 'http://localhost:8080',
  [string]$Salida = ''
)

Add-Type -AssemblyName System.Net.Http
$http = New-Object System.Net.Http.HttpClient
$http.Timeout = [TimeSpan]::FromSeconds(30)
$API = "$($Gateway.TrimEnd('/'))/api/v1"
$script:ok = 0
$script:fallas = 0
$script:lineas = New-Object System.Collections.Generic.List[string]

function Anotar([string]$texto) {
  Write-Host $texto
  $script:lineas.Add($texto)
}

# Llama a la API y devuelve @{ Estado = 200; Json = <objeto> } sin lanzar excepciones en 4xx/5xx
function Llamar([string]$metodo, [string]$ruta, [string]$token = '', $cuerpo = $null, [string]$clave = '') {
  $url = if ($ruta.StartsWith('http')) { $ruta } else { "$API$ruta" }
  $req = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::new($metodo), $url)
  if ($token) { $req.Headers.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer', $token) }
  if ($clave) { [void]$req.Headers.TryAddWithoutValidation('Idempotency-Key', $clave) }
  if ($null -ne $cuerpo) {
    $json = $cuerpo | ConvertTo-Json -Depth 6 -Compress
    $req.Content = New-Object System.Net.Http.StringContent($json, [System.Text.Encoding]::UTF8, 'application/json')
  }
  $res = $http.SendAsync($req).GetAwaiter().GetResult()
  $texto = $res.Content.ReadAsStringAsync().GetAwaiter().GetResult()
  $obj = $null
  if ($texto) { try { $obj = $texto | ConvertFrom-Json } catch { $obj = $texto } }
  return @{ Estado = [int]$res.StatusCode; Json = $obj }
}

function Comprobar([string]$nombre, [bool]$condicion, [string]$detalle = '') {
  if ($condicion) { $script:ok++; Anotar "  [OK]    $nombre $detalle" }
  else { $script:fallas++; Anotar "  [FALLA] $nombre $detalle" }
}

function Esperar([string]$nombre, $respuesta, [int]$estado) {
  Comprobar $nombre ($respuesta.Estado -eq $estado) "(esperado $estado, recibido $($respuesta.Estado))"
}

function Login([string]$correo, [string]$password) {
  $r = Llamar 'POST' '/auth/login' '' @{ correo = $correo; password = $password }
  Esperar "login $correo" $r 200
  return $r.Json.token
}

function Stock([string]$codigo) { (Llamar 'GET' "/productos/$codigo").Json.stock }

function Compra([string]$codigo, [int]$cantidad, $cupon = $null) {
  return @{
    cliente   = @{ nombre = 'Camila'; apellidos = 'Rojas Vera'; correo = 'camila.rojas@gmail.com' }
    direccion = @{ calle = 'Av. Providencia 100'; region = 'Región Metropolitana de Santiago'; comuna = 'Providencia' }
    items     = @(@{ codigo = $codigo; cantidad = $cantidad })
    cupon     = $cupon
  }
}

Anotar "Prueba de humo Sonido Vivo - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') - Gateway $Gateway"

Anotar ''
Anotar '1. Gateway y sesión de los tres roles'
$salud = Llamar 'GET' "$($Gateway.TrimEnd('/'))/actuator/health"
Esperar 'gateway /actuator/health' $salud 200
$admin = Login 'maria.perez@gmail.com' 'admin123'
$vendedor = Login 'carlos.munoz@duoc.cl' 'vend123'
$cliente = Login 'camila.rojas@gmail.com' 'cli123'
Esperar 'login con contraseña incorrecta' (Llamar 'POST' '/auth/login' '' @{ correo = 'maria.perez@gmail.com'; password = 'mala' }) 401
$yo = Llamar 'GET' '/auth/me' $vendedor
Comprobar '/auth/me del vendedor' ($yo.Estado -eq 200 -and $yo.Json.tipoUsuario -eq 'Vendedor') "(tipoUsuario=$($yo.Json.tipoUsuario))"

Anotar ''
Anotar '2. Catálogo público'
$productos = Llamar 'GET' '/productos'
Comprobar 'GET /productos sin sesión' ($productos.Estado -eq 200 -and $productos.Json.Count -ge 51) "($($productos.Json.Count) productos)"
$categorias = Llamar 'GET' '/categorias'
Comprobar 'GET /categorias sin sesión' ($categorias.Estado -eq 200 -and $categorias.Json.Count -ge 10) "($($categorias.Json.Count) categorías)"
# Producto con stock suficiente para las compras de la prueba
$p = $productos.Json | Where-Object { $_.stock -ge 4 } | Select-Object -First 1
$codigo = $p.codigo
Anotar "  Producto de prueba: $codigo ($($p.nombre)), stock inicial $($p.stock), precio vigente $($p.precioVigente)"

Anotar ''
Anotar '3. Compras y stock'
$s0 = Stock $codigo
$sinSesion = Llamar 'POST' '/boletas' '' (Compra $codigo 1)
Comprobar 'compra SIN sesión' ($sinSesion.Estado -eq 201 -and $null -eq $sinSesion.Json.cliente.run) "(boleta $($sinSesion.Json.id), run=null, total $($sinSesion.Json.total))"
$s1 = Stock $codigo
Comprobar 'stock descontado en 1' ($s1 -eq $s0 - 1) "($s0 -> $s1)"

$conSesion = Llamar 'POST' '/boletas' $cliente (Compra $codigo 1 'SONIDOVIVO10')
Comprobar 'compra CON sesión y cupón' ($conSesion.Estado -eq 201 -and $conSesion.Json.cliente.run -eq '33333333-3' -and $conSesion.Json.descuento -gt 0) "(boleta $($conSesion.Json.id), run=$($conSesion.Json.cliente.run), descuento $($conSesion.Json.descuento))"
$s2 = Stock $codigo
Comprobar 'stock descontado en 1' ($s2 -eq $s1 - 1) "($s1 -> $s2)"
$mias = Llamar 'GET' '/usuarios/me/boletas' $cliente
Comprobar 'la compra aparece en Mis compras' ($mias.Estado -eq 200 -and (@($mias.Json | Where-Object { $_.id -eq $conSesion.Json.id }).Count -eq 1))

$sinStock = Llamar 'POST' '/boletas' '' (Compra $codigo ($s2 + 1))
Comprobar 'compra con stock insuficiente' ($sinStock.Estado -eq 409) "(409: $($sinStock.Json.mensaje))"
Comprobar 'el stock no cambió tras el 409' ((Stock $codigo) -eq $s2)

$clave = [guid]::NewGuid().ToString()
$i1 = Llamar 'POST' '/boletas' '' (Compra $codigo 1) $clave
$i2 = Llamar 'POST' '/boletas' '' (Compra $codigo 1) $clave
Comprobar 'Idempotency-Key: el reintento devuelve la misma boleta' ($i1.Estado -eq 201 -and $i2.Json.id -eq $i1.Json.id) "(boleta $($i1.Json.id) y $($i2.Json.id))"
Comprobar 'Idempotency-Key: el stock se descontó una sola vez' ((Stock $codigo) -eq $s2 - 1)

Anotar ''
Anotar '4. Permisos del Vendedor (solo lista y detalle de productos y órdenes)'
Esperar 'vendedor GET /productos' (Llamar 'GET' '/productos' $vendedor) 200
Esperar "vendedor GET /productos/$codigo" (Llamar 'GET' "/productos/$codigo" $vendedor) 200
Esperar 'vendedor GET /boletas' (Llamar 'GET' '/boletas' $vendedor) 200
Esperar "vendedor GET /boletas/$($conSesion.Json.id)" (Llamar 'GET' "/boletas/$($conSesion.Json.id)" $vendedor) 200
Esperar 'vendedor GET /productos/criticos' (Llamar 'GET' '/productos/criticos' $vendedor) 403
Esperar 'vendedor POST /productos' (Llamar 'POST' '/productos' $vendedor @{ codigo = 'HUMO1'; nombre = 'X'; categoriaId = 'accesorios'; precio = 1; stock = 1; oferta = $false }) 403
Esperar "vendedor DELETE /productos/$codigo" (Llamar 'DELETE' "/productos/$codigo" $vendedor) 403
Esperar 'vendedor POST /categorias' (Llamar 'POST' '/categorias' $vendedor @{ nombre = 'Humo' }) 403
Esperar 'vendedor GET /usuarios' (Llamar 'GET' '/usuarios' $vendedor) 403
Esperar 'vendedor GET /usuarios/me' (Llamar 'GET' '/usuarios/me' $vendedor) 403
Esperar 'vendedor GET /usuarios/me/boletas' (Llamar 'GET' '/usuarios/me/boletas' $vendedor) 403
Esperar 'vendedor GET /reportes/ventas' (Llamar 'GET' '/reportes/ventas' $vendedor) 403

Anotar ''
Anotar '5. Cliente, Administrador, invitado y rutas internas'
Esperar 'cliente GET /boletas (lista de todas)' (Llamar 'GET' '/boletas' $cliente) 403
Esperar "cliente GET /boletas/$($sinSesion.Json.id) (boleta ajena)" (Llamar 'GET' "/boletas/$($sinSesion.Json.id)" $cliente) 403
Esperar 'cliente GET /reportes/ventas' (Llamar 'GET' '/reportes/ventas' $cliente) 403
$reporte = Llamar 'GET' '/reportes/ventas' $admin
Comprobar 'admin GET /reportes/ventas' ($reporte.Estado -eq 200 -and $reporte.Json.cantidadOrdenes -ge 3 -and $null -ne $reporte.Json.unidadesEnStock) "($($reporte.Json.cantidadOrdenes) boletas, total $($reporte.Json.totalVendido), unidadesEnStock $($reporte.Json.unidadesEnStock))"
Esperar 'admin GET /productos/criticos' (Llamar 'GET' '/productos/criticos' $admin) 200
Esperar 'admin GET /usuarios' (Llamar 'GET' '/usuarios' $admin) 200
Esperar 'admin GET /usuarios/33333333-3/boletas' (Llamar 'GET' '/usuarios/33333333-3/boletas' $admin) 200
Esperar 'invitado GET /boletas' (Llamar 'GET' '/boletas') 401
Esperar 'POST /internal/ventas desde fuera' (Llamar 'POST' "$($Gateway.TrimEnd('/'))/internal/ventas" '' @{ referencia = 'x'; items = @() }) 404

Anotar ''
Anotar "Resultado: $($script:ok) comprobaciones OK, $($script:fallas) fallas"
if ($Salida) {
  $dir = Split-Path -Parent $Salida
  if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Force $dir | Out-Null }
  [IO.File]::WriteAllLines((Join-Path (Get-Location) $Salida), $script:lineas, (New-Object System.Text.UTF8Encoding($false)))
  Write-Host "Evidencia guardada en $Salida"
}
if ($script:fallas -gt 0) { exit 1 }
