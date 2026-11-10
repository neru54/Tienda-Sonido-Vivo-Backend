package cl.sonidovivo.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales para iniciar sesión")
public record LoginRequest(
		@Schema(example = "maria.perez@gmail.com") @NotBlank(message = "El correo es obligatorio.") String correo,
		@Schema(example = "admin123") @NotBlank(message = "La contraseña es obligatoria.") String password) {
}
