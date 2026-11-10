package cl.sonidovivo.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.sun.net.httpserver.HttpServer;

/**
 * El Gateway real frente a tres microservicios FALSOS (servidores HTTP del JDK) que responden
 * "<servicio> <ruta>". Comprueba a quién llega cada ruta, que /internal no se publica y el CORS.
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
		r.add("app.cors-origins", () -> "http://localhost:5173,http://mi-bucket.s3-website-us-east-1.amazonaws.com");
	}

	@AfterAll
	static void detener() {
		USUARIOS.stop(0);
		CATALOGO.stop(0);
		PEDIDOS.stop(0);
	}

	@Autowired
	WebTestClient web;

	@LocalServerPort
	int puerto;

	void llegaA(String ruta, String esperado) {
		web.get().uri(ruta).exchange().expectStatus().isOk().expectBody(String.class).isEqualTo(esperado);
	}

	@Test
	void cadaRutaLlegaAlServicioDuenoDelRecurso() {
		llegaA("/api/v1/auth/me", "usuarios /api/v1/auth/me");
		llegaA("/api/v1/usuarios", "usuarios /api/v1/usuarios");
		llegaA("/api/v1/usuarios/me", "usuarios /api/v1/usuarios/me");
		llegaA("/api/v1/usuarios/11111111-1", "usuarios /api/v1/usuarios/11111111-1");
		llegaA("/api/v1/productos", "catalogo /api/v1/productos");
		llegaA("/api/v1/productos/criticos", "catalogo /api/v1/productos/criticos");
		llegaA("/api/v1/categorias/baterias", "catalogo /api/v1/categorias/baterias");
		llegaA("/api/v1/boletas", "pedidos /api/v1/boletas");
		llegaA("/api/v1/boletas/1001", "pedidos /api/v1/boletas/1001");
		llegaA("/api/v1/reportes/ventas", "pedidos /api/v1/reportes/ventas");
	}

	@Test
	void elHistorialBajoUsuariosLoAtiendePedidos() {
		llegaA("/api/v1/usuarios/me/boletas", "pedidos /api/v1/usuarios/me/boletas");
		llegaA("/api/v1/usuarios/33333333-3/boletas", "pedidos /api/v1/usuarios/33333333-3/boletas");
	}

	@Test
	void reenviaElTokenYLosDocumentosDeSwagger() {
		web.get().uri("/api/v1/boletas").header(HttpHeaders.AUTHORIZATION, "Bearer abc").exchange()
				.expectBody(String.class).isEqualTo("pedidos /api/v1/boletas con-token");
		llegaA("/openapi/catalogo", "catalogo /v3/api-docs");
		llegaA("/openapi/pedidos", "pedidos /v3/api-docs");
	}

	@Test
	void lasRutasInternasNoSePublican() {
		web.post().uri("/internal/ventas").exchange().expectStatus().isNotFound();
		web.get().uri("/internal/inventario").exchange().expectStatus().isNotFound();
	}

	/**
	 * Preflight CORS con java.net.http (como un navegador) contra el puerto real. Con WebTestClient
	 * Spring rechaza el preflight como "origin is malformed" por cómo arma la petición de prueba.
	 */
	HttpResponse<Void> preflight(String origen) throws Exception {
		HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/v1/boletas"))
				.method("OPTIONS", HttpRequest.BodyPublishers.noBody())
				.header("Origin", origen)
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "authorization,content-type,idempotency-key")
				.build();
		return HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.discarding());
	}

	@Test
	void corsPermiteSoloLosOrigenesConfigurados() throws Exception {
		String s3 = "http://mi-bucket.s3-website-us-east-1.amazonaws.com";
		HttpResponse<Void> permitido = preflight(s3);
		assertThat(permitido.statusCode()).isEqualTo(200);
		assertThat(permitido.headers().firstValue("Access-Control-Allow-Origin")).contains(s3);
		assertThat(permitido.headers().firstValue("Access-Control-Allow-Headers").orElse("").toLowerCase())
				.contains("authorization").contains("idempotency-key");

		assertThat(preflight("http://sitio-ajeno.com").statusCode()).isEqualTo(403);
	}

	@Test
	void saludParaDockerYSwaggerUi() {
		web.get().uri("/actuator/health").exchange().expectStatus().isOk();
		web.get().uri("/swagger-ui.html").exchange().expectStatus().is3xxRedirection();
	}
}
