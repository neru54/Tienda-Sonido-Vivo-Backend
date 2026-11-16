package cl.sonidovivo.gateway;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.sun.net.httpserver.HttpServer;

/**
 * El Gateway real frente a tres microservicios FALSOS (servidores HTTP del JDK) que responden
 * "<servicio> <ruta>". Comprueba a quién llega cada ruta y que el token se reenvía.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class RutasGatewayTest {

	static final HttpServer USUARIOS = falso("usuarios");
	static final HttpServer CATALOGO = falso("catalogo");
	static final HttpServer PEDIDOS = falso("pedidos");

	static HttpServer falso(String nombre) {
		try {
			HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			s.createContext("/", ex -> {
				String auth = ex.getRequestHeaders().getFirst("Authorization");
				byte[] cuerpo = (nombre + " " + ex.getRequestURI().getPath() + (auth == null ? "" : " con-token"))
						.getBytes(StandardCharsets.UTF_8);
				ex.sendResponseHeaders(200, cuerpo.length);
				ex.getResponseBody().write(cuerpo);
				ex.close();
			});
			s.start();
			return s;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	static String url(HttpServer s) {
		return "http://127.0.0.1:" + s.getAddress().getPort();
	}

	@DynamicPropertySource
	static void urls(DynamicPropertyRegistry r) {
		r.add("app.usuarios-url", () -> url(USUARIOS));
		r.add("app.catalogo-url", () -> url(CATALOGO));
		r.add("app.pedidos-url", () -> url(PEDIDOS));
	}

	@AfterAll
	static void detener() {
		USUARIOS.stop(0);
		CATALOGO.stop(0);
		PEDIDOS.stop(0);
	}

	@Autowired
	WebTestClient web;

	void llegaA(String ruta, String esperado) {
		web.get().uri(ruta).exchange().expectStatus().isOk().expectBody(String.class).isEqualTo(esperado);
	}

	@Test
	void cadaRutaLlegaAlServicioDuenoDelRecurso() {
		llegaA("/api/v1/auth/me", "usuarios /api/v1/auth/me");
		llegaA("/api/v1/usuarios", "usuarios /api/v1/usuarios");
		llegaA("/api/v1/usuarios/me", "usuarios /api/v1/usuarios/me");
		llegaA("/api/v1/usuarios/11111111-1", "usuarios /api/v1/usuarios/11111111-1");
		llegaA("/api/v1/usuarios/me/boletas", "pedidos /api/v1/usuarios/me/boletas");
		llegaA("/api/v1/productos", "catalogo /api/v1/productos");
		llegaA("/api/v1/productos/criticos", "catalogo /api/v1/productos/criticos");
		llegaA("/api/v1/categorias/baterias", "catalogo /api/v1/categorias/baterias");
		llegaA("/api/v1/boletas", "pedidos /api/v1/boletas");
		llegaA("/api/v1/boletas/1001", "pedidos /api/v1/boletas/1001");
		llegaA("/api/v1/reportes/ventas", "pedidos /api/v1/reportes/ventas");
	}

	@Test
	void reenviaElTokenYLosDocumentosDeSwagger() {
		web.get().uri("/api/v1/boletas").header(HttpHeaders.AUTHORIZATION, "Bearer abc").exchange()
				.expectBody(String.class).isEqualTo("pedidos /api/v1/boletas con-token");
		llegaA("/openapi/catalogo", "catalogo /v3/api-docs");
		llegaA("/openapi/pedidos", "pedidos /v3/api-docs");
	}
}
