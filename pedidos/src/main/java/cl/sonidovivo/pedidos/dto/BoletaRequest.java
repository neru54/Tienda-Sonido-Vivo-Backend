package cl.sonidovivo.pedidos.dto;

import java.util.List;

import cl.sonidovivo.common.util.Patrones;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Compra enviada desde el checkout. El frontend solo manda QUÉ productos y CUÁNTOS:
 * precios, descuento y total los calcula el backend con los datos de la base (no se confía en el cliente).
 */
@Schema(description = "Datos de la compra (checkout)")
public record BoletaRequest(
		@NotNull(message = "Faltan los datos del cliente.") @Valid Cliente cliente,
		@NotNull(message = "Falta la dirección de entrega.") @Valid Direccion direccion,
		@NotEmpty(message = "El carrito está vacío.") List<@Valid Item> items,
		@Schema(example = "SONIDOVIVO10", description = "Opcional") String cupon) {

	@Schema(name = "ClienteCompra")
	public record Cliente(
			@Schema(example = "Camila") @NotBlank(message = "El nombre es obligatorio.") @Size(max = 50) String nombre,
			@Schema(example = "Rojas Vera") @NotBlank(message = "Los apellidos son obligatorios.") @Size(max = 100) String apellidos,
			@Schema(example = "camila.rojas@gmail.com") @NotBlank(message = "El correo es obligatorio.") @Pattern(regexp = Patrones.CORREO_PERMITIDO, message = Patrones.MSJ_CORREO) String correo) {
	}

	@Schema(name = "DireccionEntrega")
	public record Direccion(
			@Schema(example = "Av. Providencia 100") @NotBlank(message = "La calle es obligatoria.") @Size(max = 150) String calle,
			@Size(max = 20) String departamento,
			@Schema(example = "Región Metropolitana de Santiago") @NotBlank(message = "Selecciona una región.") String region,
			@Schema(example = "Providencia") @NotBlank(message = "Selecciona una comuna.") String comuna,
			@Size(max = 300) String indicaciones) {
	}

	@Schema(name = "ItemCompra")
	public record Item(
			@Schema(example = "GA001") @NotBlank(message = "Falta el código del producto.") String codigo,
			@Schema(example = "2") @NotNull @Min(value = 1, message = "La cantidad debe ser al menos 1.") Integer cantidad) {
	}
}
