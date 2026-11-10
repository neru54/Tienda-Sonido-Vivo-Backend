package cl.sonidovivo.pedidos.dto;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Boleta con su detalle. MISMA forma que en el monolito (cliente, direccion, items...) para que el
 * frontend no cambie. La construye mapper/BoletaMapper a partir de la entidad.
 */
@Schema(description = "Boleta (cabecera + detalle)")
public record BoletaResponse(
		@Schema(example = "1001") Long id,
		Instant fecha,
		@Schema(example = "Pagada") String estado,
		Cliente cliente,
		Direccion direccion,
		List<Detalle> items,
		@Schema(example = "SONIDOVIVO10") String cupon,
		@Schema(example = "259980") int subtotal,
		@Schema(example = "25998") int descuento,
		@Schema(example = "233982") int total) {

	@Schema(name = "ClienteBoleta")
	public record Cliente(@Schema(description = "null si se compró sin cuenta") String run, String nombre, String apellidos, String correo) {
	}

	@Schema(name = "DireccionBoleta")
	public record Direccion(String calle, String departamento, String region, String comuna, String indicaciones) {
	}

	@Schema(name = "DetalleBoleta")
	public record Detalle(String codigo, String nombre, @Schema(description = "Categoría al momento de la venta") String categoria,
			@Schema(description = "Precio unitario cobrado") int precio, int cantidad, int subtotal) {
	}
}
