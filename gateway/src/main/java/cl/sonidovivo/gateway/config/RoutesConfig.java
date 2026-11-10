package cl.sonidovivo.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tabla de rutas: qué microservicio atiende cada parte de /api/v1.
 *
 *  /api/v1/usuarios/me/boletas, /api/v1/usuarios/{run}/boletas -> pedidos   (order -1: se evalúan ANTES
 *                                                                           que /usuarios/**; son boletas)
 *  /api/v1/auth/**, /api/v1/usuarios/**                         -> usuarios
 *  /api/v1/productos/**, /api/v1/categorias/**                  -> catalogo
 *  /api/v1/boletas/**, /api/v1/reportes/**                      -> pedidos
 *  /openapi/{servicio}                                          -> /v3/api-docs del servicio (Swagger)
 *
 * /internal/** NO tiene ruta: desde fuera responde 404. Así el navegador no puede llamar al
 * descuento de stock aunque conociera la clave interna.
 */
@Configuration
public class RoutesConfig {

	@Bean
	RouteLocator rutas(RouteLocatorBuilder b,
			@Value("${app.usuarios-url}") String usuarios,
			@Value("${app.catalogo-url}") String catalogo,
			@Value("${app.pedidos-url}") String pedidos) {
		return b.routes()
				.route("pedidos-historial", r -> r.order(-1)
						.path("/api/v1/usuarios/me/boletas", "/api/v1/usuarios/*/boletas").uri(pedidos))
				.route("usuarios", r -> r.path("/api/v1/auth/**", "/api/v1/usuarios", "/api/v1/usuarios/**").uri(usuarios))
				.route("catalogo", r -> r.path("/api/v1/productos", "/api/v1/productos/**",
						"/api/v1/categorias", "/api/v1/categorias/**").uri(catalogo))
				.route("pedidos", r -> r.path("/api/v1/boletas", "/api/v1/boletas/**", "/api/v1/reportes/**").uri(pedidos))
				.route("docs-usuarios", r -> r.path("/openapi/usuarios").filters(f -> f.setPath("/v3/api-docs")).uri(usuarios))
				.route("docs-catalogo", r -> r.path("/openapi/catalogo").filters(f -> f.setPath("/v3/api-docs")).uri(catalogo))
				.route("docs-pedidos", r -> r.path("/openapi/pedidos").filters(f -> f.setPath("/v3/api-docs")).uri(pedidos))
				.build();
	}
}
