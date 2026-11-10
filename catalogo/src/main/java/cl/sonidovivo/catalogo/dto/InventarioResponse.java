package cl.sonidovivo.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Resumen de bodega para el reporte de ventas de pedidos (uso interno). */
@Schema(description = "Resumen de inventario (uso interno)")
public record InventarioResponse(
		@Schema(example = "230") long unidadesEnStock,
		@Schema(example = "51") long productos) {
}
