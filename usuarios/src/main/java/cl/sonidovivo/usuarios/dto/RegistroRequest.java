package cl.sonidovivo.usuarios.dto;

import java.time.LocalDate;

import cl.sonidovivo.common.util.Patrones;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear una cuenta de cliente desde la tienda")
public record RegistroRequest(
		@Schema(example = "12345678-5") @NotBlank(message = "El RUN es obligatorio.") @Pattern(regexp = Patrones.RUN, message = Patrones.MSJ_RUN) String run,
		@Schema(example = "Ana") @NotBlank(message = "El nombre es obligatorio.") @Size(max = 50) String nombre,
		@Schema(example = "Soto Díaz") @NotBlank(message = "Los apellidos son obligatorios.") @Size(max = 100) String apellidos,
		@Schema(example = "ana.soto@gmail.com") @NotBlank(message = "El correo es obligatorio.") @Size(max = 100) @Pattern(regexp = Patrones.CORREO_PERMITIDO, message = Patrones.MSJ_CORREO) String correo,
		@Schema(example = "abcd1234") @NotBlank(message = "La contraseña es obligatoria.") @Size(min = 4, max = 10, message = Patrones.MSJ_PASSWORD) String password,
		@Schema(example = "Región de Valparaíso") @NotBlank(message = "Selecciona una región.") String region,
		@Schema(example = "Quilpué") @NotBlank(message = "Selecciona una comuna.") String comuna,
		@Schema(example = "Los Carrera 123") @NotBlank(message = "La dirección es obligatoria.") @Size(max = 300) String direccion,
		@Schema(example = "2000-01-31") LocalDate fechaNacimiento) {
}
