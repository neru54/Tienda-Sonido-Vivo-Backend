package cl.sonidovivo.usuarios.dto;

import java.time.LocalDate;

import cl.sonidovivo.common.security.TipoUsuario;
import cl.sonidovivo.common.util.Patrones;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta o edición de un usuario desde el panel (solo Administrador).
 * Al crear, la contraseña es obligatoria; al editar puede venir vacía y entonces se conserva la anterior
 * (esa regla la aplica UsuarioServicio). En la edición el RUN de la URL manda: el del cuerpo se ignora.
 */
@Schema(description = "Datos de usuario para el panel de administración")
public record UsuarioRequest(
		@Schema(example = "12345678-5") @NotBlank(message = "El RUN es obligatorio.") @Pattern(regexp = Patrones.RUN, message = Patrones.MSJ_RUN) String run,
		@Schema(example = "Pedro") @NotBlank(message = "El nombre es obligatorio.") @Size(max = 50) String nombre,
		@Schema(example = "Díaz") @NotBlank(message = "Los apellidos son obligatorios.") @Size(max = 100) String apellidos,
		@Schema(example = "pedro.diaz@duoc.cl") @NotBlank(message = "El correo es obligatorio.") @Size(max = 100) @Pattern(regexp = Patrones.CORREO_PERMITIDO, message = Patrones.MSJ_CORREO) String correo,
		@Schema(description = "Obligatoria al crear; opcional al editar", example = "vend123") @Size(max = 10, message = Patrones.MSJ_PASSWORD) String password,
		LocalDate fechaNacimiento,
		@Schema(example = "Vendedor") @NotNull(message = "Selecciona el tipo de usuario.") TipoUsuario tipoUsuario,
		@NotBlank(message = "Selecciona una región.") String region,
		@NotBlank(message = "Selecciona una comuna.") String comuna,
		@NotBlank(message = "La dirección es obligatoria.") @Size(max = 300) String direccion) {
}
