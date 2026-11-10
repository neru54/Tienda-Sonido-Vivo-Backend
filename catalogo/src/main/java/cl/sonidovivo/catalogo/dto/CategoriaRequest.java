package cl.sonidovivo.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** El id no se envía: el backend lo deriva del nombre al crear y no cambia al renombrar. */
@Schema(description = "Datos de una categoría")
public record CategoriaRequest(
		@Schema(example = "Vientos y Metales") @NotBlank(message = "El nombre es obligatorio.") @Size(max = 50, message = "Máximo 50 caracteres.") String nombre,
		@Size(max = 200, message = "Máximo 200 caracteres.") String descripcion,
		@Schema(example = "#2A241C") @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "El color debe tener el formato #RRGGBB.") String color) {
}
