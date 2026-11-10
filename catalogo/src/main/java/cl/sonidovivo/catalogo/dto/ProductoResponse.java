package cl.sonidovivo.catalogo.dto;

import cl.sonidovivo.catalogo.model.Producto;
import io.swagger.v3.oas.annotations.media.Schema;

/** Producto tal como lo entrega la API. "categoria" es el nombre (lo que muestra la tienda) y "categoriaId" la FK. */
@Schema(description = "Producto del catálogo")
public record ProductoResponse(
		@Schema(example = "GA001") String codigo,
		@Schema(example = "Guitarra Acústica Folk") String nombre,
		@Schema(example = "guitarras-acusticas") String categoriaId,
		@Schema(example = "Guitarras Acústicas") String categoria,
		String marca,
		String modelo,
		String descripcion,
		String imagen,
		@Schema(example = "129990") int precio,
		@Schema(example = "8") int stock,
		@Schema(example = "3") int stockCritico,
		boolean oferta,
		@Schema(example = "110490") Integer precioOferta,
		@Schema(description = "Precio que se cobra (de oferta si corresponde)", example = "110490") int precioVigente,
		@Schema(description = "true si stock <= stockCritico") boolean critico) {

	public static ProductoResponse desde(Producto p) {
		return new ProductoResponse(p.getCodigo(), p.getNombre(), p.getCategoria().getId(), p.getCategoria().getNombre(),
				p.getMarca(), p.getModelo(), p.getDescripcion(), p.getImagen(), p.getPrecio(), p.getStock(),
				p.getStockCritico(), p.isOferta(), p.getPrecioOferta(), p.precioVigente(), p.esStockCritico());
	}
}
