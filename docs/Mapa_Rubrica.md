# Mapa de requisitos de la rúbrica — Evaluación Parcial 3

Versión 3.1 · 10-11-2026 · Sonido Vivo (DSY1104)

Cada fila indica **dónde está la evidencia**: archivo, prueba o comando. Las rutas de archivo son relativas a `fase3/backend-microservicios` (backend) o a `fase3/proyecto_Etapa3_REACT` (frontend, marcadas *FE*). Los pesos son los de la pauta tal como los resume la referencia del docente; el 60 % restante corresponde a la presentación individual.

## Indicadores técnicos

| Indicador | Peso | Qué pide | Evidencia |
|---|---:|---|---|
| **IE3.1.1 Backend y base de datos** | 8 % | Spring Boot, BD relacional (MySQL), entidades y relaciones, repositorios y servicios, lógica de negocio probada | 3 microservicios Spring Boot 3.5 + Gateway + `common` (`pom.xml`); MySQL 8 con 3 esquemas y un usuario por servicio (`sql/01_crear_esquemas_mysql.sql`, `compose.yaml`); entidades `Usuario`, `Categoria`→`Producto`, `VentaRegistrada`→`VentaLinea`, `Boleta`→`DetalleBoleta`; repositorios Spring Data con JPQL y bloqueo pesimista; reglas en `UsuarioService`, `ProductoService`, `CategoriaService`, `VentaService` y `BoletaService`; **97 pruebas JUnit en verde, 92,6 % de líneas (JaCoCo)**. Modelo por esquema: `docs/APIs_e_Integracion.md` §2 |
| **IE3.2.1 CRUD y Swagger** | 8 % | CRUD REST versionado `/api/v1` documentado con Swagger | CRUD completo de productos, categorías y usuarios; boletas (crear y consultar) y reportes; 27 operaciones bajo `/api/v1` (`docs/APIs_e_Integracion.md` §4). Swagger agregado: `http://localhost:8080/swagger-ui.html` con selector Usuarios, Catálogo y Pedidos; especificaciones en `docs/openapi/*.json`. Pruebas: `RutasGatewayTest.reenviaElTokenYLosDocumentosDeSwagger`, `saludParaDockerYSwaggerUi`. Capturas 34 y 35 del manual |
| **IE3.2.2 Integración REST** | 6 % | El frontend consume la API con axios/fetch | *FE* `src/api/cliente.js` (axios, `VITE_API_URL` = Gateway) y un servicio por recurso en `src/api/*`; **6/6 pruebas de integración real** (`npm run test:integracion`) contra el Gateway con MySQL; **humo 38/38** (`docs/evidencias/humo-docker-mysql.txt`); llamada interna pedidos→catálogo (`CatalogoCliente`, `docs/APIs_e_Integracion.md` §5) |
| **IE3.3.1 JWT y roles** | 6 % | Spring Security + JWT, roles Administrador, Vendedor y Cliente | `common/security/JwtUtil.java` (HS256, `sub` = RUN, claim `rol`), `FiltroJwt`, `SeguridadBase`; `SecurityConfig` de cada servicio con la matriz de permisos (`docs/APIs_e_Integracion.md` §3.3); BCrypt en `usuarios`. Pruebas: `JwtUtilTest`, `FiltrosTest`, `UsuariosApiTest`, `CatalogoApiTest.vendedorSoloLeeProductos`, `PedidosApiTest.permisosDelVendedor…`; humo, sección 4 (Vendedor: 4 lecturas permitidas y 8 rutas con 403) |
| **IE3.3.2 Persistencia de sesión** | 6 % | La sesión se mantiene de forma segura | *FE* `SessionContext`: token y usuario en `sessionStorage` (sobreviven a recargar y se borran al cerrar la pestaña); interceptor que agrega `Authorization: Bearer` y cierra la sesión ante un 401 (token vencido, 2 h por defecto); `GET /auth/me` para comprobar la sesión. Pruebas *FE* `api` (interceptor, sesión expirada), `paginas` (token en cada petición); BE `UsuariosApiTest.authMeRequiereTokenYSirveAlVendedor` |
| **IE3.3.3 Restricciones en el frontend** | 6 % | Cada rol ve solo lo suyo | *FE* `src/utils/roles.js` (permisos por rol), `RequierePermiso.jsx`, `AdminSidebar.jsx`, `AdminLayout.jsx`: el Vendedor solo ve *Productos* y *Órdenes / Boletas*, sin botones de crear, editar ni eliminar; el Cliente no entra al panel; redirección por rol tras el login. Pruebas *FE* `vendedor.test.jsx`, `App.test.jsx` (vendedor redirigido a Productos). Capturas 30 a 33 del manual |

## Requisitos de la pauta y del docente

| Requisito | Evidencia |
|---|---|
| **Microservicios (exigencia del docente)** | Estructura igual a la referencia: Maven multimódulo `common` + `usuarios` + `catalogo` + `pedidos` + `gateway`; `application.yml`, `application-h2.yml` y `application-mysql.yml` por servicio; `Dockerfile` con `ARG MODULE`; `compose.yaml`; `.env.example`; `sql/`; `docs/`. Paquetes `controller`, `service`, `repository`, `model`, `dto`, `exception`, `config` (+ `client`, `mapper`) |
| BD relacional permitida (MySQL; no PostgreSQL ni Supabase) | Perfiles `h2` (desarrollo) y `mysql` (Docker y RDS). Sin dependencias de PostgreSQL |
| API versionada | Prefijo `/api/v1` en todas las rutas públicas; las rutas internas `/internal/**` no se publican |
| Repositorio GitHub público del backend con commits claros | Repositorio Git propio en `backend-microservicios`, con un commit por fase en formato Conventional Commits (`git log --oneline`). La publicación en GitHub la hace el equipo |
| ERS | `docs/ERS_v3.md` (v3.1: requisitos RF-45 a RF-53 de microservicios, RNF-15 y RNF-16, matriz de trazabilidad) |
| Manual de usuario con pantallazos | `docs/Manual_de_Usuario.md` por rol, con 38 marcadores `![Pantalla N](capturas/…)` y la lista de capturas en la sección 7. **Las imágenes las toma el equipo** |
| Documentación de APIs | Swagger del Gateway + `docs/openapi/usuarios.json`, `catalogo.json`, `pedidos.json` |
| Documento APIs e Integración | `docs/APIs_e_Integracion.md`: diagrama de arquitectura, modelo por esquema (Mermaid), endpoints por servicio y rol, flujo JWT, llamada interna y pruebas con resultados reales |
| Despliegue (AWS Academy) | `aws/README_AWS.md` + `aws/aprovisionar.ps1`, `aws/compose.rds.yaml`, `aws/ec2-user-data.sh`; frontend con *FE* `aws/deploy-s3.ps1 -ApiUrl …`. Preparado y **no ejecutado**; plan B local probado (humo 38/38 en H2) |

## Comandos para reproducir la evidencia

```powershell
cd fase3\backend-microservicios
.\mvnw.cmd clean verify                                           # 97 pruebas + JaCoCo
docker compose up --build -d                                      # MySQL + 4 servicios
powershell -ExecutionPolicy Bypass -File scripts\humo.ps1         # 38 comprobaciones
cd ..\proyecto_Etapa3_REACT
npm test                                                          # 192 pruebas
npm run test:integracion                                          # 6 pruebas contra el Gateway
```
