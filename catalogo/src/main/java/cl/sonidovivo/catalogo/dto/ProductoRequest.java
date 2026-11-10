package cl.sonidovivo.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta o edición de producto (solo Administrador). Al editar, el código de la URL manda
 * y el del cuerpo se ignora (el código no se puede cambiar).
 */
@Schema(description = "Datos de un producto")
public record ProductoRequest(
		@Schema(example = "AC010") @NotBlank(message = "El código es obligatorio.") @Size(min = 3, max = 20, message = "El código debe tener entre 3 y 20 caracteres.") String codigo,
		@Schema(example = "Cable de instrumento 3 m") @NotBlank(message = "El nombre es obligatorio.") @Size(max = 100) String nombre,
		@Schema(example = "accesorios") @NotBlank(message = "Selecciona una categoría.") String categoriaId,
		@Size(max = 60) String marca,
		@Size(max = 60) String modelo,
		@Size(max = 500, message = "Máximo 500 caracteres.") String descripcion,
		@Size(max = 500) String imagen,
		@Schema(example = "9990") @NotNull(message = "El precio es obligatorio.") @Min(value = 0, message = "El precio no puede ser negativo.") Integer precio,
		@Schema(example = "10") @NotNull(message = "El stock es obligatorio.") @Min(value = 0, message = "El stock no puede ser negativo.") Integer stock,
		@Schema(description = "Si no se envía se usa 3", example = "3") @Min(value = 0, message = "El stock crítico no puede ser negativo.") Integer stockCritico,
		boolean oferta,
		@Schema(description = "Obligatorio si oferta = true; no puede superar el precio") @Min(0) Integer precioOferta) {
}
