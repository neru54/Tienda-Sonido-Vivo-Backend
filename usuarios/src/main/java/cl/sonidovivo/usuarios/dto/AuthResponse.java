package cl.sonidovivo.usuarios.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token JWT y datos del usuario autenticado")
public record AuthResponse(
		@Schema(description = "JWT a enviar en el encabezado Authorization: Bearer <token>") String token,
		@Schema(example = "Bearer") String tipo,
		@Schema(description = "Fecha y hora en que el token deja de ser válido") Instant expiraEn,
		UsuarioResponse usuario) {
}
