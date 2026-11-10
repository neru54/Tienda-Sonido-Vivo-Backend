package cl.sonidovivo.catalogo.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Petición INTERNA de pedidos -> catalogo: "descuenta este stock para la boleta X".
 * Solo viajan códigos y cantidades: el precio lo pone catalogo desde su base.
 * "referencia" es el número de boleta y hace la operación idempotente.
 */
@Schema(description = "Venta a registrar (uso interno de pedidos)")
public record VentaRequest(
		@Schema(example = "1001") @NotBlank(message = "Falta la referencia de la venta.") @Size(max = 64) String referencia,
		@NotEmpty(message = "El carrito está vacío.") List<@Valid Item> items) {

	@Schema(name = "ItemVenta")
	public record Item(
			@Schema(example = "GA001") @NotBlank(message = "Falta el código del producto.") String codigo,
			@Schema(example = "2") @NotNull @Min(value = 1, message = "La cantidad debe ser al menos 1.") Integer cantidad) {
	}
}
