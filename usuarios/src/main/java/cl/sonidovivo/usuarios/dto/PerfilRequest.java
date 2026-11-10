package cl.sonidovivo.usuarios.dto;

import java.time.LocalDate;

import cl.sonidovivo.common.util.Patrones;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lo que una persona puede cambiar de su propia cuenta. RUN, correo y tipo de usuario NO están: no se pueden auto-modificar. */
@Schema(description = "Datos editables del propio perfil")
public record PerfilRequest(
		@NotBlank(message = "El nombre es obligatorio.") @Size(max = 50) String nombre,
		@NotBlank(message = "Los apellidos son obligatorios.") @Size(max = 100) String apellidos,
		@Schema(description = "Opcional: si viene vacía se conserva la actual") @Size(max = 10, message = Patrones.MSJ_PASSWORD) String password,
		LocalDate fechaNacimiento,
		@NotBlank(message = "Selecciona una región.") String region,
		@NotBlank(message = "Selecciona una comuna.") String comuna,
		@NotBlank(message = "La dirección es obligatoria.") @Size(max = 300) String direccion) {
}
