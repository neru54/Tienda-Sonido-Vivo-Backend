package cl.sonidovivo.pedidos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import cl.sonidovivo.common.exception.ApiException;
import cl.sonidovivo.common.exception.ServicioNoDisponibleException;
import cl.sonidovivo.pedidos.client.CatalogoCliente;
import cl.sonidovivo.pedidos.client.VentaCatalogo;

/**
 * El cliente HTTP real contra un "catalogo" falso (servidor HTTP del JDK): envía la clave interna
 * y traduce cada respuesta (201, 409, 401, sin conexión) a lo que espera BoletaService.
 */
class CatalogoClienteTest {

	HttpServer servidor;
	final AtomicReference<String> claveRecibida = new AtomicReference<>();
	volatile int estado = 201;
	volatile String cuerpo = "";

	@BeforeEach
	void levantar() throws IOException {
		servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		servidor.createContext("/internal/", this::responder);
		servidor.start();
	}

	@AfterEach
	void detener() {
		servidor.stop(0);
	}

	private void responder(HttpExchange ex) throws IOException {
		claveRecibida.set(ex.getRequestHeaders().getFirst("X-Internal-Api-Key"));
		byte[] datos = cuerpo.getBytes(StandardCharsets.UTF_8);
		ex.getResponseHeaders().add("Content-Type", "application/json");
		ex.sendResponseHeaders(estado, datos.length == 0 ? -1 : datos.length);
		if (datos.length > 0) ex.getResponseBody().write(datos);
		ex.close();
	}

	CatalogoCliente cliente(String url) {
		ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
		return new CatalogoCliente(RestClient.builder(), mapper, url, "clave-secreta", 2000);
	}

	String url() {
		return "http://127.0.0.1:" + servidor.getAddress().getPort();
	}

	static final VentaCatalogo.Peticion PETICION = new VentaCatalogo.Peticion("1001", List.of(new VentaCatalogo.Item("GA001", 2)));

	@Test
	void ventaConfirmadaYClaveInternaEnviada() {
		cuerpo = """
				{"referencia":"1001","fecha":"2026-11-10T15:00:00Z","subtotal":220980,
				 "lineas":[{"codigo":"GA001","nombre":"Guitarra Folk","categoria":"Guitarras Acústicas","cantidad":2,"precioUnitario":110490,"subtotal":220980}]}
				""";
		var r = cliente(url()).registrarVenta(PETICION);

		assertThat(r.subtotal()).isEqualTo(220_980);
		assertThat(r.lineas().get(0).categoria()).isEqualTo("Guitarras Acústicas");
		assertThat(claveRecibida.get()).isEqualTo("clave-secreta");
	}

	@Test
	void conflictoDeStockConservaCodigoYMensaje() {
		estado = 409;
		cuerpo = "{\"estado\":409,\"mensaje\":\"No hay stock suficiente de \\\"Guitarra Folk\\\" (disponible: 1).\"}";

		assertThatThrownBy(() -> cliente(url()).registrarVenta(PETICION))
				.isInstanceOf(ApiException.class)
				.isNotInstanceOf(ServicioNoDisponibleException.class)
				.hasMessage("No hay stock suficiente de \"Guitarra Folk\" (disponible: 1).")
				.extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void claveMalConfiguradaOErrorInternoSonServicioNoDisponible() {
		estado = 401;
		cuerpo = "{\"mensaje\":\"Debes iniciar sesión\"}";
		assertThatThrownBy(() -> cliente(url()).registrarVenta(PETICION)).isInstanceOf(ServicioNoDisponibleException.class);
		estado = 500;
		assertThatThrownBy(() -> cliente(url()).registrarVenta(PETICION)).isInstanceOf(ServicioNoDisponibleException.class);
	}

	@Test
	void catalogoApagadoEsServicioNoDisponibleYElInventarioQuedaVacio() {
		String apagado = url();
		servidor.stop(0);

		assertThatThrownBy(() -> cliente(apagado).registrarVenta(PETICION)).isInstanceOf(ServicioNoDisponibleException.class);
		assertThat(cliente(apagado).unidadesEnStock()).isEmpty();
	}

	@Test
	void inventarioDevuelveLasUnidades() {
		estado = 200;
		cuerpo = "{\"unidadesEnStock\":230,\"productos\":51}";
		assertThat(cliente(url()).unidadesEnStock()).contains(230L);
	}
}
