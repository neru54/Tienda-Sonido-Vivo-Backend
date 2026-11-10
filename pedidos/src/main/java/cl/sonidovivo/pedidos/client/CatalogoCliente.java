package cl.sonidovivo.pedidos.client;

import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.sonidovivo.common.exception.ApiException;
import cl.sonidovivo.common.exception.ServicioNoDisponibleException;
import cl.sonidovivo.common.security.ClaveInternaFiltro;

/**
 * Cliente HTTP hacia el microservicio catalogo (llamada INTERNA, no pasa por el Gateway).
 *
 *  - URL: CATALOGO_URL (en Docker http://catalogo:8082, en local http://localhost:8082).
 *  - Autenticación: encabezado X-Internal-Api-Key con INTERNAL_API_KEY (no se reenvía el JWT del cliente:
 *    quien autoriza el descuento de stock es el servicio pedidos, no el navegador).
 *  - Tiempos máximos de espera para que una caída de catalogo no deje colgado el checkout.
 *
 * Traducción de errores:
 *  - 400/404/409 de catalogo -> la MISMA excepción con el mismo mensaje (ej. "No hay stock suficiente...").
 *    Son respuestas definitivas: reintentar no cambia el resultado.
 *  - Sin conexión, tiempo agotado, 5xx o 401 (clave mal configurada) -> ServicioNoDisponibleException (503).
 */
@Component
public class CatalogoCliente {

	private static final Logger log = LoggerFactory.getLogger(CatalogoCliente.class);

	private final RestClient http;
	private final ObjectMapper mapper;

	public CatalogoCliente(RestClient.Builder builder, ObjectMapper mapper,
			@Value("${app.catalogo.url}") String url,
			@Value("${app.internal.api-key}") String claveInterna,
			@Value("${app.catalogo.timeout-ms:5000}") long timeoutMs) {
		SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
		fabrica.setConnectTimeout(Duration.ofMillis(Math.min(timeoutMs, 3000)));
		fabrica.setReadTimeout(Duration.ofMillis(timeoutMs));
		this.http = builder.baseUrl(url)
				.requestFactory(fabrica)
				.defaultHeader(ClaveInternaFiltro.ENCABEZADO, claveInterna)
				.build();
		this.mapper = mapper;
	}

	/** Pide a catalogo validar y descontar el stock de la boleta "referencia". */
	public VentaCatalogo.Respuesta registrarVenta(VentaCatalogo.Peticion peticion) {
		try {
			return http.post().uri("/internal/ventas")
					.contentType(MediaType.APPLICATION_JSON)
					.body(peticion)
					.retrieve()
					.body(VentaCatalogo.Respuesta.class);
		} catch (RestClientResponseException e) {
			throw traducir(e);
		} catch (ResourceAccessException e) {
			log.warn("Catálogo no respondió al registrar la venta {}: {}", peticion.referencia(), e.getMessage());
			throw new ServicioNoDisponibleException("El catálogo no está disponible en este momento.");
		}
	}

	/** Unidades en bodega para el reporte. Vacío si catalogo no responde (el reporte sigue sin ese dato). */
	public Optional<Long> unidadesEnStock() {
		try {
			VentaCatalogo.Inventario inv = http.get().uri("/internal/inventario").retrieve().body(VentaCatalogo.Inventario.class);
			return Optional.ofNullable(inv).map(VentaCatalogo.Inventario::unidadesEnStock);
		} catch (RuntimeException e) {
			log.warn("No se pudo obtener el inventario de catálogo: {}", e.getMessage());
			return Optional.empty();
		}
	}

	private RuntimeException traducir(RestClientResponseException e) {
		HttpStatus estado = HttpStatus.resolve(e.getStatusCode().value());
		if (estado == HttpStatus.BAD_REQUEST || estado == HttpStatus.NOT_FOUND || estado == HttpStatus.CONFLICT) {
			return new ApiException(estado, mensaje(e));
		}
		log.error("Catálogo respondió {} a la venta interna: {}", e.getStatusCode(), e.getResponseBodyAsString());
		return new ServicioNoDisponibleException("El catálogo no está disponible en este momento.");
	}

	/** Lee el campo "mensaje" del ErrorRespuesta de catalogo (mismo formato en todos los servicios). */
	private String mensaje(RestClientResponseException e) {
		try {
			JsonNode cuerpo = mapper.readTree(e.getResponseBodyAsString());
			if (cuerpo.hasNonNull("mensaje")) return cuerpo.get("mensaje").asText();
		} catch (Exception ignorada) {
			// cuerpo no JSON: se usa el mensaje genérico
		}
		return "El catálogo rechazó la compra.";
	}
}
