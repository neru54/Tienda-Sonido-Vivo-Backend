package cl.sonidovivo.pedidos.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resumen de ventas (mismos campos que el monolito). unidadesEnStock es el único dato que no está
 * en pedidos: se pide a catalogo y queda en null si catalogo no responde (el resto del reporte sigue).
 */
@Schema(description = "Resumen de ventas para el panel de reportes")
public record ReporteVentasResponse(
		@Schema(example = "12") int cantidadOrdenes,
		@Schema(example = "1850000") long totalVendido,
		@Schema(example = "154167") long ticketPromedio,
		@Schema(example = "230", description = "null si catalogo no respondió") Long unidadesEnStock,
		@Schema(description = "Top 5 por unidades vendidas") List<ProductoVendido> topProductos,
		@Schema(description = "Monto vendido por categoría, de mayor a menor") List<VentaCategoria> porCategoria) {

	public record ProductoVendido(String codigo, String nombre, long unidades, long monto) {
	}

	public record VentaCategoria(String categoria, long monto) {
	}
}
