# ---------------------------------------------------------------
# PLAN B sin Docker ni MySQL: levanta los 4 servicios con el perfil h2 (base de archivo local).
# Requiere haber compilado antes:  .\mvnw.cmd -DskipTests package
#   powershell -ExecutionPolicy Bypass -File scripts/iniciar-h2.ps1
#   powershell -ExecutionPolicy Bypass -File scripts/iniciar-h2.ps1 -Base 18080   (puertos 18080..18083)
#   powershell -ExecutionPolicy Bypass -File scripts/iniciar-h2.ps1 -Detener
# Gateway en http://localhost:<Base>. Los datos H2 y los logs quedan en scripts/salida/<servicio>/.
# Usa los secretos de desarrollo de application.yml (no usar este modo con datos reales).
# ---------------------------------------------------------------
param(
  [int]$Base = 8080,
  [switch]$Detener
)

$raiz = Split-Path -Parent $PSScriptRoot
$salida = Join-Path $PSScriptRoot 'salida'
$archivoPids = Join-Path $salida 'pids.txt'

if ($Detener) {
  # Se detiene por PUERTO: en Windows "java" suele ser un lanzador (javapath) que crea otro proceso,
  # así que el PID que devuelve Start-Process no siempre es el de la JVM real.
  if (Test-Path $archivoPids) { $Base = [int](Get-Content $archivoPids -TotalCount 1) }
  foreach ($puerto in $Base..($Base + 3)) {
    Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue |
      ForEach-Object { try { Stop-Process -Id $_.OwningProcess -Force -ErrorAction Stop } catch {} }
  }
  if (Test-Path $archivoPids) { Remove-Item $archivoPids }
  Write-Host "Servicios detenidos (puertos $Base a $($Base + 3))."
  return
}

$servicios = @(
  @{ Nombre = 'usuarios'; Puerto = $Base + 1 },
  @{ Nombre = 'catalogo'; Puerto = $Base + 2 },
  @{ Nombre = 'pedidos'; Puerto = $Base + 3 },
  @{ Nombre = 'gateway'; Puerto = $Base }
)
New-Item -ItemType Directory -Force $salida | Out-Null
$pids = @("$Base") # primera línea: puerto base (lo usa -Detener)

# Direcciones internas: pedidos llama a catalogo y el Gateway a los tres
$env:CATALOGO_URL = "http://localhost:$($Base + 2)"
$env:USUARIOS_URL = "http://localhost:$($Base + 1)"
$env:PEDIDOS_URL = "http://localhost:$($Base + 3)"

foreach ($s in $servicios) {
  $jar = Join-Path $raiz "$($s.Nombre)\target\$($s.Nombre)-1.0.0.jar"
  if (-not (Test-Path $jar)) { throw "No existe $jar. Compila primero con: .\mvnw.cmd -DskipTests package" }
  $dir = Join-Path $salida $s.Nombre
  New-Item -ItemType Directory -Force $dir | Out-Null
  $env:PORT = "$($s.Puerto)"
  # Cada servicio en su propia carpeta: su base H2 (./data) no se mezcla con la de los otros
  $p = Start-Process java -ArgumentList '-jar', "`"$jar`"" -WorkingDirectory $dir -PassThru -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $dir 'salida.log') -RedirectStandardError (Join-Path $dir 'error.log')
  $pids += $p.Id
  Write-Host "Iniciando $($s.Nombre) en el puerto $($s.Puerto) (PID $($p.Id))"
  if ($s.Nombre -eq 'catalogo') { Start-Sleep -Seconds 2 }
}
$pids | Set-Content $archivoPids

# Esperar a que todos respondan /actuator/health
foreach ($s in $servicios) {
  $listo = $false
  for ($i = 0; $i -lt 90 -and -not $listo; $i++) {
    try { $listo = (Invoke-WebRequest -UseBasicParsing "http://localhost:$($s.Puerto)/actuator/health" -TimeoutSec 2).StatusCode -eq 200 } catch { Start-Sleep -Seconds 1 }
  }
  if (-not $listo) { throw "$($s.Nombre) no respondió. Revisa scripts/salida/$($s.Nombre)/salida.log" }
  Write-Host "  $($s.Nombre) listo"
}
Write-Host ''
Write-Host "Gateway: http://localhost:$Base/api/v1   Swagger: http://localhost:$Base/swagger-ui.html" -ForegroundColor Green
Write-Host 'Para detener: powershell -ExecutionPolicy Bypass -File scripts/iniciar-h2.ps1 -Detener'
