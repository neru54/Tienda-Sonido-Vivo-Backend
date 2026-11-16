package cl.sonidovivo.common.exception;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Formato único de error de TODOS los microservicios.
 * El frontend solo lee el campo "mensaje", así que no importa qué servicio respondió.
 */
@Schema(description = "Respuesta de error")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorRespuesta(
		@Schema(example = "2026-11-06T15:00:00Z") Instant fecha,
		@Schema(example = "409") int estado,
		@Schema(example = "Conflict") String error,
		@Schema(example = "Ya existe un producto con ese código.") String mensaje,
		@Schema(example = "/api/v1/productos") String ruta,
		@Schema(description = "Errores por campo (solo en validaciones)") Map<String, String> errores) {

	public static ErrorRespuesta de(int estado, String error, String mensaje, String ruta) {
		return new ErrorRespuesta(Instant.now(), estado, error, mensaje, ruta, null);
	}
}
