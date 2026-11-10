# Manual de Usuario — Tienda Sonido Vivo

Versión 3.1 (microservicios) · 10-11-2026 · DSY1104 Desarrollo Fullstack II · Evaluación Parcial 3

> **Qué cambió en la v3.1.** Para quien usa la tienda, las pantallas son las mismas que en la v3.0. Lo que cambió es el servidor: ahora son tres microservicios (usuarios, catálogo y pedidos) detrás de un API Gateway, con una base MySQL de tres esquemas. Se actualizaron la sección 1.3 (cómo levantar el sistema), la 5 (Swagger con selector de servicio) y algunos mensajes. La lista de capturas que debe tomar el equipo está al final.

Este manual explica cómo usar la tienda online **Sonido Vivo** y su panel de administración, según el rol de cada persona:

| Rol | Qué puede hacer | Sección |
|---|---|---|
| Visitante (sin cuenta) | Ver el catálogo, armar el carrito y **comprar sin registrarse** | 2 |
| Cliente | Lo mismo, con sus datos autocompletados y sus compras guardadas en su cuenta | 2 |
| Vendedor | Ver productos y órdenes en el panel | 4 |
| Administrador | Todo: productos, categorías, usuarios, órdenes, reportes y perfil | 3 |

> **Capturas.** Cada `![Pantalla N — …](capturas/N-….png)` apunta a una imagen que el equipo debe guardar en `docs/capturas/` con ese nombre exacto. La línea en cursiva de abajo dice qué debe verse. La [lista completa de capturas](#7-lista-de-capturas-por-tomar) está al final.

---

## 1. Antes de empezar

### 1.1 Acceso
- **Tienda:** `http://localhost:5173` en local, o la dirección del sitio S3 publicada por el equipo (`http://<bucket>.s3-website-us-east-1.amazonaws.com`).
- **Navegadores:** Chrome, Edge, Firefox o Safari recientes, en computador, tablet o celular.
- La tienda necesita el servidor en funcionamiento: el **API Gateway** en el puerto 8080 y, detrás, los microservicios usuarios, catálogo y pedidos. Si no lo está, las páginas muestran *"No se pudo conectar con el servidor…"* con un botón **Reintentar** (ver sección 6).

### 1.2 Cuentas de demostración

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | maria.perez@gmail.com | admin123 |
| Vendedor | carlos.munoz@duoc.cl | vend123 |
| Cliente | camila.rojas@gmail.com | cli123 |

### 1.3 Para el equipo: levantar el sistema
1. Backend con Docker y MySQL. En `fase3/backend-microservicios`, copia `.env.example` como `.env`, reemplaza los valores `REEMPLAZAR_...` y ejecuta `docker compose up --build -d`. Espera a que `docker compose ps` muestre los 5 contenedores como *healthy*.
   - Sin Docker (H2): `.\mvnw.cmd -DskipTests package` y luego `powershell -ExecutionPolicy Bypass -File scripts/iniciar-h2.ps1`.
2. Frontend: en `fase3/proyecto_Etapa3_REACT`, ejecuta `npm install` y luego `npm run dev`. Usa `VITE_API_URL=http://localhost:8080/api/v1`, que es el valor por defecto.
3. Abre `http://localhost:5173`.

En AWS el procedimiento está en `aws/README_AWS.md`.

![Pantalla 01 — Sistema en funcionamiento](capturas/01-sistema-en-funcionamiento.png)
*Pantalla 01. Terminal con `docker compose ps` mostrando mysql, usuarios, catalogo, pedidos y gateway en estado "healthy" (solo gateway con el puerto 0.0.0.0:8080), y otra terminal con `npm run dev` mostrando `http://localhost:5173`.*

---

## 2. Tienda (visitantes y clientes)

### 2.1 Portada
La portada muestra el mensaje de la tienda, el botón **Explorar productos**, las **categorías** (cada tarjeta lleva a sus productos) y las **ofertas destacadas**. Mientras se cargan los datos aparece *"Cargando…"*.

![Pantalla 02 — Portada](capturas/02-portada.png)
*Pantalla 02. Página de inicio completa: hero con "Hecho por músicos. Hecho para sonar.", la grilla "Explora las categorías" y las ofertas destacadas.*

En la barra superior están siempre: el **Carrito** (con la cantidad de unidades) e **Iniciar sesión** (o **Cerrar sesión** si ya entraste). En celular, el resto del menú se abre con el botón ☰.

![Pantalla 03 — Barra de navegación en celular](capturas/03-barra-de-navegacion-en-celular.png)
*Pantalla 03. Vista móvil (ancho ≈ 375 px) con el menú ☰ abierto, mostrando Carrito e Iniciar sesión siempre visibles.*

### 2.2 Buscar y filtrar productos
1. Entra a **Productos**.
2. Escribe en el buscador: filtra al instante por nombre, marca, modelo o código.
3. Elige una categoría en la lista **Todas las categorías** para acotar.
4. Debajo se indica cuántos productos coinciden. Si no hay coincidencias verás *"No hay productos"*.

![Pantalla 04 — Catálogo con búsqueda](capturas/04-catalogo-con-busqueda.png)
*Pantalla 04. Página Productos con "yamaha" escrito en el buscador y el contador "N producto(s)".*

Las etiquetas de cada tarjeta indican: **Oferta** (precio normal tachado y precio rebajado), **Stock crítico** (quedan pocas unidades) y **Sin stock** (no se puede añadir).

### 2.3 Categorías y ofertas
- **Categorías:** elige una tarjeta para ver solo sus productos. La dirección de la página cambia (por ejemplo `/categorias/baterias`), así puedes compartir el enlace.
- **Ofertas:** lista todos los productos rebajados.

![Pantalla 05 — Categoría seleccionada](capturas/05-categoria-seleccionada.png)
*Pantalla 05. Página Categorías con "Baterías" marcada y sus productos debajo.*

### 2.4 Detalle de un producto
Haz clic en un producto para ver su foto, marca, modelo, descripción, precio y stock disponible. Elige la **Cantidad** (no puede superar el stock) y pulsa **Añadir al carrito**; aparecerá *"Se añadieron N unidad(es) al carrito."* y el contador de la barra subirá.

![Pantalla 06 — Detalle de producto](capturas/06-detalle-de-producto.png)
*Pantalla 06. Ficha de "Guitarra Acústica Folk" con el precio de oferta, el selector de cantidad y el mensaje de confirmación verde.*

### 2.5 Carrito y cupones
En **Carrito** puedes:
- Subir o bajar cantidades con **+** y **−** (si llega a 0, el producto se quita).
- **Eliminar** un producto o **Limpiar carrito**.
- Aplicar un **cupón de descuento** en *¿Tienes un cupón?* → **Aplicar cupón**. Cupones vigentes: `SONIDOVIVO10` (10 %) y `BIENVENIDO5` (5 %). Si es válido verás *"Cupón aplicado correctamente."* y el descuento; si no, *"El cupón ingresado no es válido."*.

El carrito se guarda en tu navegador: no se pierde si recargas la página.

![Pantalla 07 — Carrito con cupón](capturas/07-carrito-con-cupon.png)
*Pantalla 07. Carrito con dos productos, el cupón SONIDOVIVO10 aplicado y el detalle subtotal / descuento / total.*

Pulsa **Ir a pagar** para continuar.

### 2.6 Comprar (checkout)
**No necesitas una cuenta para comprar.** Si no iniciaste sesión verás el aviso *"Puedes comprar sin cuenta. Inicia sesión para guardar la compra en tu historial."*.

1. Completa **Datos del cliente** (nombre, apellidos, correo) y **Dirección de entrega** (calle, departamento opcional, región, comuna e indicaciones opcionales). Si iniciaste sesión, estos datos ya vienen completados.
2. Revisa el **Resumen de la compra** a la derecha.
3. Pulsa **Pagar ahora**. El botón cambia a *"Procesando pago…"* mientras el servidor confirma la compra.

![Pantalla 08 — Checkout como invitado](capturas/08-checkout-como-invitado.png)
*Pantalla 08. Formulario de checkout sin sesión, con el aviso "Puedes comprar sin cuenta" y el resumen a la derecha.*

![Pantalla 09 — Checkout con sesión](capturas/09-checkout-con-sesion.png)
*Pantalla 09. Mismo formulario con la sesión de Camila iniciada: datos ya completados.*

**Si algún dato está mal**, el campo se marca en rojo con una explicación y no se envía nada.
**Si un producto ya no tiene stock suficiente**, verás por ejemplo *"No hay stock suficiente de "Guitarra Acústica Folk" (disponible: 1)."*; tu carrito se conserva para que ajustes la cantidad.

![Pantalla 10 — Error de stock](capturas/10-error-de-stock.png)
*Pantalla 10. Checkout mostrando el aviso rojo de stock insuficiente.*

### 2.7 Confirmación de la compra
Si el pago se realiza verás *"Se ha realizado la compra nro #N"*, la fecha, los datos de entrega y el detalle con los montos finales. Puedes:
- **Imprimir boleta** (abre la impresión del navegador sin menús ni botones).
- **Enviar por email** (demostración: confirma el envío al correo indicado).
- **Seguir comprando**.

El carrito queda vacío.

![Pantalla 11 — Compra realizada](capturas/11-compra-realizada.png)
*Pantalla 11. Página "Se ha realizado la compra nro #…" con el resumen y los tres botones.*

![Pantalla 12 — Boleta impresa](capturas/12-boleta-impresa.png)
*Pantalla 12. Vista previa de impresión de la boleta (sin barra de navegación).*

**Pago con error (demostración):** marca *"Simular error de pago"* antes de pagar para ver la pantalla *"No se pudo realizar el pago"*; tu carrito se conserva y puedes pulsar **Volver a realizar el pago**.

![Pantalla 13 — Pago con error](capturas/13-pago-con-error.png)
*Pantalla 13. Pantalla "No se pudo realizar el pago" con el botón "Volver a realizar el pago".*

### 2.8 Crear una cuenta
1. Pulsa **Registrarse** en la barra.
2. Completa RUN (sin puntos y con guion, ej. `12345678-5`), correo (`@duoc.cl`, `@profesor.duoc.cl` o `@gmail.com`), nombre, apellidos, región, comuna (se habilita al elegir la región), dirección y contraseña (4 a 10 caracteres) dos veces.
3. Pulsa **Crear cuenta**. Quedarás con la sesión iniciada como cliente.

Los errores aparecen mientras escribes. Si el RUN o el correo ya están registrados verás *"Ya existe un usuario con ese RUN."* o *"Ya existe un usuario con ese correo."*.

![Pantalla 14 — Registro con errores](capturas/14-registro-con-errores.png)
*Pantalla 14. Formulario de registro con un RUN inválido y un correo @hotmail.com marcados en rojo.*

### 2.9 Iniciar y cerrar sesión
1. Pulsa **Iniciar sesión**, escribe correo y contraseña y pulsa **Ingresar**.
2. Según tu rol: el cliente vuelve a la tienda (su nombre aparece en la barra), el administrador entra al **Dashboard** y el vendedor a **Productos** del panel.
3. Si los datos no coinciden verás *"Correo o contraseña incorrectos."*.
4. Para salir, pulsa **Cerrar sesión**.

Por seguridad, la sesión se cierra sola al cerrar la pestaña o después de 2 horas; en ese caso verás *"Tu sesión expiró. Vuelve a iniciar sesión."*.

![Pantalla 15 — Inicio de sesión](capturas/15-inicio-de-sesion.png)
*Pantalla 15. Formulario de login con el mensaje "Correo o contraseña incorrectos."*

![Pantalla 16 — Cliente con sesión](capturas/16-cliente-con-sesion.png)
*Pantalla 16. Barra de navegación con el nombre "Camila" y el botón "Cerrar sesión".*

### 2.10 Blogs, Nosotros y Contacto
- **Blogs:** artículos sobre instrumentos; cada uno enlaza a la categoría relacionada.
- **Nosotros:** historia y equipo de la tienda.
- **Contacto:** nombre y comentario (máx. 500 caracteres, con contador) son obligatorios; el correo es opcional. Al enviar verás la confirmación.

![Pantalla 17 — Contacto](capturas/17-contacto.png)
*Pantalla 17. Formulario de contacto con el contador de caracteres y el mensaje de envío confirmado.*

---

## 3. Panel de administración (Administrador)

Inicia sesión con una cuenta de administrador. También puedes entrar desde el botón **Admin** de la barra. El menú lateral tiene: Dashboard, Órdenes / Boletas, Productos, Productos críticos, Categorías, Usuarios, Reportes, Perfil y Volver a la tienda.

### 3.1 Dashboard
Muestra la cantidad de **compras** y el total vendido, la cantidad de **productos** y cuántos están en stock crítico, y la cantidad de **usuarios**, con accesos rápidos a las tareas más comunes.

![Pantalla 18 — Dashboard](capturas/18-dashboard.png)
*Pantalla 18. Panel del administrador con las tres tarjetas de métricas, los accesos rápidos y el menú lateral completo.*

### 3.2 Órdenes / Boletas
La lista muestra las compras de la más reciente a la más antigua: número, fecha, cliente (con la etiqueta **Invitado** si se compró sin cuenta), estado y total. **Ver boleta** abre el detalle, que se puede imprimir.

![Pantalla 19 — Lista de órdenes](capturas/19-lista-de-ordenes.png)
*Pantalla 19. Tabla de órdenes con al menos una compra "Invitado".*

![Pantalla 20 — Detalle de boleta](capturas/20-detalle-de-boleta.png)
*Pantalla 20. Boleta #N con cliente, dirección de entrega, detalle de productos, descuento y total.*

### 3.3 Productos
- **Buscar:** escribe en el buscador sobre la tabla.
- **Ver:** ficha de solo lectura del producto.
- **Nuevo producto:** completa código (3 a 20 caracteres, único), nombre, categoría, precio y stock (números enteros, sin puntos), y opcionalmente marca, modelo, descripción, URL de imagen y stock crítico (si lo dejas vacío se usa 3). Si marcas **Producto en oferta**, indica el precio de oferta (no puede ser mayor al precio normal). Pulsa **Crear producto**.
- **Editar:** el código no se puede cambiar. Pulsa **Guardar cambios**.
- **Eliminar:** pulsa **Eliminar** y confirma con **Sí**. Un producto que ya se vendió no se puede eliminar (*"…tiene ventas registradas. Puedes dejar su stock en 0."*).

![Pantalla 21 — Lista de productos (administrador)](capturas/21-lista-de-productos-administrador.png)
*Pantalla 21. Tabla con los botones Ver, Editar y Eliminar, y el botón "Nuevo producto".*

![Pantalla 22 — Nuevo producto](capturas/22-nuevo-producto.png)
*Pantalla 22. Formulario con "Producto en oferta" marcado y el campo de precio de oferta visible.*

![Pantalla 23 — Confirmar eliminación](capturas/23-confirmar-eliminacion.png)
*Pantalla 23. Fila de un producto con la confirmación "¿Eliminar? Sí / No".*

### 3.4 Productos críticos
Lista los productos cuyo stock es menor o igual a su stock crítico, para reponerlos (usa **Editar** para actualizar el stock).

![Pantalla 24 — Productos críticos](capturas/24-productos-criticos.png)
*Pantalla 24. Tabla con las etiquetas "Stock crítico".*

### 3.5 Categorías
- **Nueva categoría:** nombre (máx. 50, no repetido) y descripción opcional.
- **Editar:** al cambiar el nombre, los productos de la categoría muestran el nombre nuevo automáticamente.
- **Eliminar:** solo si la categoría no tiene productos; si los tiene verás *"No se puede eliminar "…" porque tiene productos asociados."*.

![Pantalla 25 — Categorías](capturas/25-categorias.png)
*Pantalla 25. Tabla de categorías con la columna "Productos" y el aviso rojo al intentar eliminar una con productos.*

### 3.6 Usuarios
- **Nuevo usuario:** RUN, nombre, apellidos, correo, **tipo de usuario** (Administrador, Vendedor o Cliente), fecha de nacimiento (opcional), región, comuna, dirección y contraseña.
- **Editar:** el RUN no se puede cambiar. Si dejas **Nueva contraseña** vacía, se conserva la actual.
- **Historial:** muestra las compras de ese usuario con enlace a cada boleta.
- **Eliminar:** no puedes eliminar tu propia cuenta mientras tienes la sesión iniciada. Las boletas de un usuario eliminado se conservan y siguen mostrando su RUN como dato histórico del comprador.

![Pantalla 26 — Usuarios](capturas/26-usuarios.png)
*Pantalla 26. Tabla de usuarios con los botones Historial, Editar y Eliminar.*

![Pantalla 27 — Historial de compras](capturas/27-historial-de-compras.png)
*Pantalla 27. Historial de Camila Rojas con sus boletas.*

### 3.7 Reportes
Muestra el total vendido y la cantidad de compras, el ticket promedio, las unidades en stock, los 5 productos más vendidos y las ventas por categoría. Las unidades en stock las informa el microservicio de catálogo; si en ese momento no responde, la tarjeta muestra **—** con la nota *"Catálogo no disponible"* y el resto del reporte se ve igual.

![Pantalla 28 — Reportes](capturas/28-reportes.png)
*Pantalla 28. Página de reportes con las tarjetas y las dos tablas, después de realizar algunas compras.*

### 3.8 Perfil
Permite editar tus propios datos (nombre, apellidos, fecha de nacimiento, región, comuna, dirección y contraseña). RUN, correo y tipo de usuario aparecen bloqueados.

![Pantalla 29 — Mi perfil](capturas/29-mi-perfil.png)
*Pantalla 29. Formulario de perfil con RUN, correo y tipo de usuario deshabilitados y el mensaje "Perfil actualizado correctamente."*

---

## 4. Panel del Vendedor

El vendedor solo puede **consultar productos y órdenes**. Al iniciar sesión entra directamente a **Productos**, y su menú lateral muestra únicamente:

- **Órdenes / Boletas:** lista de compras y detalle de cada boleta (igual que en 3.2, incluida la impresión).
- **Productos:** lista con buscador y botón **Ver** para la ficha de cada producto. No aparecen los botones Nuevo producto, Editar ni Eliminar.

Si un vendedor escribe a mano la dirección de otra sección del panel (por ejemplo `/admin/usuarios`), verá *"No tienes permisos para ver esta sección."* con un botón para volver.

![Pantalla 30 — Menú del vendedor](capturas/30-menu-del-vendedor.png)
*Pantalla 30. Panel con la sesión de Carlos (Vendedor): el menú lateral solo con "Órdenes / Boletas", "Productos" y "Volver a la tienda".*

![Pantalla 31 — Productos (vendedor)](capturas/31-productos-vendedor.png)
*Pantalla 31. Lista de productos con solo el botón "Ver" en cada fila.*

![Pantalla 32 — Ficha de producto (vendedor)](capturas/32-ficha-de-producto-vendedor.png)
*Pantalla 32. Detalle de un producto en el panel, sin botón Editar.*

![Pantalla 33 — Acceso no permitido](capturas/33-acceso-no-permitido.png)
*Pantalla 33. Vendedor en `/admin/usuarios` viendo "No tienes permisos para ver esta sección."*

---

## 5. Documentación de la API (equipo técnico)

Con el backend en funcionamiento, la documentación interactiva está en el Gateway: `http://localhost:8080/swagger-ui.html`. Arriba a la derecha hay un **selector** con un documento por microservicio: *Usuarios (auth y cuentas)*, *Catálogo (productos y categorías)* y *Pedidos (boletas y reportes)*.

1. En el selector elige **Usuarios**, abre **Autenticación → POST /api/v1/auth/login**, pulsa **Try it out**, escribe correo y contraseña y pulsa **Execute**.
2. Copia el valor de `token` de la respuesta.
3. Pulsa **Authorize** (arriba a la derecha), pega el token y confirma.
. Al cambiar de documento en el selector (por ejemplo, de Usuarios a Pedidos), pulsa **Authorize** otra vez con el mismo token. Los tres servicios aceptan el mismo JWT porque comparten el secreto.

![Pantalla 34 — Swagger UI](capturas/34-swagger-ui.png)
*Pantalla 34. Swagger del Gateway con el selector de servicio desplegado (Usuarios, Catálogo, Pedidos) y el documento de Usuarios abierto.*

![Pantalla 35 — Probar un endpoint](capturas/35-probar-un-endpoint.png)
*Pantalla 35. Documento "Pedidos" en Swagger: POST /api/v1/boletas ejecutado con la respuesta 201 y el número de boleta (desde 1001).*

El detalle de cada endpoint está en el documento **APIs e Integración**.

### 5.1 Despliegue en AWS (si el equipo lo publica)

![Pantalla 37 — Tienda publicada en S3](capturas/37-tienda-publicada-en-s3.png)
*Pantalla 37. La tienda abierta desde la URL `http://<bucket>.s3-website-us-east-1.amazonaws.com`, con el catálogo cargado desde la API de la EC2.*

![Pantalla 38 — Contenedores en la EC2](capturas/38-contenedores-en-la-ec2.png)
*Pantalla 38. Sesión SSH en la EC2 con `docker compose --env-file .env -f aws/compose.rds.yaml ps`: usuarios, catalogo, pedidos y gateway en estado "healthy".*

---

## 6. Mensajes frecuentes y qué hacer

| Mensaje | Qué significa | Qué hacer |
|---|---|---|
| *Cargando…* | La página está pidiendo los datos al servidor | Esperar unos segundos |
| *No se pudo conectar con el servidor. Revisa tu conexión o que la API esté en ejecución.* | El servidor no responde | Revisar la conexión o avisar al equipo; luego pulsar **Reintentar** |
| *Correo o contraseña incorrectos.* | Los datos de acceso no coinciden | Revisar mayúsculas y el correo completo |
| *Tu sesión expiró. Vuelve a iniciar sesión.* | Pasaron más de 2 horas o se cerró la pestaña | Iniciar sesión otra vez |
| *No tienes permisos para ver esta sección.* | Tu rol no tiene acceso a esa parte del panel | Usar las secciones del menú lateral |
| *No hay stock suficiente de "…" (disponible: N).* | Otra persona compró antes o el stock cambió | Bajar la cantidad en el carrito |
| *El cupón "…" no es válido.* | El cupón no existe | Revisar el código o quitar el cupón |
| *Ya existe un usuario con ese correo / RUN.* | La cuenta ya está registrada | Iniciar sesión o usar otros datos |
| *No se puede eliminar … porque tiene productos asociados.* | La categoría aún tiene productos | Mover o eliminar esos productos primero |
| *No se puede eliminar el producto porque tiene ventas registradas.* | El producto aparece en boletas | Dejar su stock en 0 |
| *No puedes eliminar tu propio usuario…* | Intentaste borrar la cuenta con la que entraste | Pedirlo a otro administrador |

![Pantalla 36 — Servidor sin conexión](capturas/36-servidor-sin-conexion.png)
*Pantalla 36. Página Productos con el aviso rojo de conexión y el botón "Reintentar".*

---

## 7. Lista de capturas por tomar

Guarda cada imagen en `docs/capturas/` con el nombre indicado. Las capturas 37 y 38 solo se necesitan si el sistema se publica en AWS.

| N.º | Archivo | Qué debe verse |
|---|---|---|
| 01 | `capturas/01-sistema-en-funcionamiento.png` | **Sistema en funcionamiento.** Terminal con `docker compose ps` mostrando mysql, usuarios, catalogo, pedidos y gateway en estado "healthy" (solo gateway con el puerto 0.0.0.0:8080), y otra terminal con `npm run dev` mostrando `http://localhost:5173`. |
| 02 | `capturas/02-portada.png` | **Portada.** Página de inicio completa: hero con "Hecho por músicos. Hecho para sonar.", la grilla "Explora las categorías" y las ofertas destacadas. |
| 03 | `capturas/03-barra-de-navegacion-en-celular.png` | **Barra de navegación en celular.** Vista móvil (ancho ≈ 375 px) con el menú ☰ abierto, mostrando Carrito e Iniciar sesión siempre visibles. |
| 04 | `capturas/04-catalogo-con-busqueda.png` | **Catálogo con búsqueda.** Página Productos con "yamaha" escrito en el buscador y el contador "N producto(s)". |
| 05 | `capturas/05-categoria-seleccionada.png` | **Categoría seleccionada.** Página Categorías con "Baterías" marcada y sus productos debajo. |
| 06 | `capturas/06-detalle-de-producto.png` | **Detalle de producto.** Ficha de "Guitarra Acústica Folk" con el precio de oferta, el selector de cantidad y el mensaje de confirmación verde. |
| 07 | `capturas/07-carrito-con-cupon.png` | **Carrito con cupón.** Carrito con dos productos, el cupón SONIDOVIVO10 aplicado y el detalle subtotal / descuento / total. |
| 08 | `capturas/08-checkout-como-invitado.png` | **Checkout como invitado.** Formulario de checkout sin sesión, con el aviso "Puedes comprar sin cuenta" y el resumen a la derecha. |
| 09 | `capturas/09-checkout-con-sesion.png` | **Checkout con sesión.** Mismo formulario con la sesión de Camila iniciada: datos ya completados. |
| 10 | `capturas/10-error-de-stock.png` | **Error de stock.** Checkout mostrando el aviso rojo de stock insuficiente. |
| 11 | `capturas/11-compra-realizada.png` | **Compra realizada.** Página "Se ha realizado la compra nro #…" con el resumen y los tres botones. |
| 12 | `capturas/12-boleta-impresa.png` | **Boleta impresa.** Vista previa de impresión de la boleta (sin barra de navegación). |
| 13 | `capturas/13-pago-con-error.png` | **Pago con error.** Pantalla "No se pudo realizar el pago" con el botón "Volver a realizar el pago". |
| 14 | `capturas/14-registro-con-errores.png` | **Registro con errores.** Formulario de registro con un RUN inválido y un correo @hotmail.com marcados en rojo. |
| 15 | `capturas/15-inicio-de-sesion.png` | **Inicio de sesión.** Formulario de login con el mensaje "Correo o contraseña incorrectos." |
| 16 | `capturas/16-cliente-con-sesion.png` | **Cliente con sesión.** Barra de navegación con el nombre "Camila" y el botón "Cerrar sesión". |
| 17 | `capturas/17-contacto.png` | **Contacto.** Formulario de contacto con el contador de caracteres y el mensaje de envío confirmado. |
| 18 | `capturas/18-dashboard.png` | **Dashboard.** Panel del administrador con las tres tarjetas de métricas, los accesos rápidos y el menú lateral completo. |
| 19 | `capturas/19-lista-de-ordenes.png` | **Lista de órdenes.** Tabla de órdenes con al menos una compra "Invitado". |
| 20 | `capturas/20-detalle-de-boleta.png` | **Detalle de boleta.** Boleta #N con cliente, dirección de entrega, detalle de productos, descuento y total. |
| 21 | `capturas/21-lista-de-productos-administrador.png` | **Lista de productos (administrador).** Tabla con los botones Ver, Editar y Eliminar, y el botón "Nuevo producto". |
| 22 | `capturas/22-nuevo-producto.png` | **Nuevo producto.** Formulario con "Producto en oferta" marcado y el campo de precio de oferta visible. |
| 23 | `capturas/23-confirmar-eliminacion.png` | **Confirmar eliminación.** Fila de un producto con la confirmación "¿Eliminar? Sí / No". |
| 24 | `capturas/24-productos-criticos.png` | **Productos críticos.** Tabla con las etiquetas "Stock crítico". |
| 25 | `capturas/25-categorias.png` | **Categorías.** Tabla de categorías con la columna "Productos" y el aviso rojo al intentar eliminar una con productos. |
| 26 | `capturas/26-usuarios.png` | **Usuarios.** Tabla de usuarios con los botones Historial, Editar y Eliminar. |
| 27 | `capturas/27-historial-de-compras.png` | **Historial de compras.** Historial de Camila Rojas con sus boletas. |
| 28 | `capturas/28-reportes.png` | **Reportes.** Página de reportes con las tarjetas y las dos tablas, después de realizar algunas compras. |
| 29 | `capturas/29-mi-perfil.png` | **Mi perfil.** Formulario de perfil con RUN, correo y tipo de usuario deshabilitados y el mensaje "Perfil actualizado correctamente." |
| 30 | `capturas/30-menu-del-vendedor.png` | **Menú del vendedor.** Panel con la sesión de Carlos (Vendedor): el menú lateral solo con "Órdenes / Boletas", "Productos" y "Volver a la tienda". |
| 31 | `capturas/31-productos-vendedor.png` | **Productos (vendedor).** Lista de productos con solo el botón "Ver" en cada fila. |
| 32 | `capturas/32-ficha-de-producto-vendedor.png` | **Ficha de producto (vendedor).** Detalle de un producto en el panel, sin botón Editar. |
| 33 | `capturas/33-acceso-no-permitido.png` | **Acceso no permitido.** Vendedor en `/admin/usuarios` viendo "No tienes permisos para ver esta sección." |
| 34 | `capturas/34-swagger-ui.png` | **Swagger UI.** Swagger del Gateway con el selector de servicio desplegado (Usuarios, Catálogo, Pedidos) y el documento de Usuarios abierto. |
| 35 | `capturas/35-probar-un-endpoint.png` | **Probar un endpoint.** Documento "Pedidos" en Swagger: POST /api/v1/boletas ejecutado con la respuesta 201 y el número de boleta (desde 1001). |
| 36 | `capturas/36-servidor-sin-conexion.png` | **Servidor sin conexión.** Página Productos con el aviso rojo de conexión y el botón "Reintentar". |
| 37 | `capturas/37-tienda-publicada-en-s3.png` | **Tienda publicada en S3.** La tienda abierta desde la URL `http://<bucket>.s3-website-us-east-1.amazonaws.com`, con el catálogo cargado desde la API de la EC2. |
| 38 | `capturas/38-contenedores-en-la-ec2.png` | **Contenedores en la EC2.** Sesión SSH en la EC2 con `docker compose --env-file .env -f aws/compose.rds.yaml ps`: usuarios, catalogo, pedidos y gateway en estado "healthy". |
