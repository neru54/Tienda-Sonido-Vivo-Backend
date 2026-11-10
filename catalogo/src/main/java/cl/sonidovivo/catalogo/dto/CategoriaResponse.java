package cl.sonidovivo.catalogo.dto;

import cl.sonidovivo.catalogo.model.Categoria;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Categoría con la cantidad de productos que tiene")
public record CategoriaResponse(
		@Schema(example = "baterias") String id,
		@Schema(example = "Baterías") String nombre,
		String descripcion,
		@Schema(example = "#4A3B5C") String color,
		@Schema(example = "5") long cantidadProductos) {

	public static CategoriaResponse desde(Categoria c, long cantidadProductos) {
		return new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion(), c.getColor(), cantidadProductos);
	}
}
