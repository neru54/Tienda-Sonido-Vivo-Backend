package cl.sonidovivo.catalogo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.sonidovivo.catalogo.dto.InventarioResponse;
import cl.sonidovivo.catalogo.dto.VentaRequest;
import cl.sonidovivo.catalogo.dto.VentaResponse;
import cl.sonidovivo.catalogo.service.VentaService;
import cl.sonidovivo.common.exception.ErrorRespuesta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Endpoints INTERNOS (servicio a servicio). Requieren el encabezado X-Internal-Api-Key y el Gateway
 * NO los publica: el navegador nunca puede descontar stock directamente.
 * Sin /api/v1 a propósito: no son parte de la API pública versionada.
 */
@RestController
@RequestMapping("/internal")
@Tag(name = "Interno", description = "Solo para el microservicio pedidos (clave X-Internal-Api-Key). No expuesto por el Gateway")
public class InternalController {

	private final VentaService servicio;

	public InternalController(VentaService servicio) {
		this.servicio = servicio;
	}

	@PostMapping("/ventas")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Registrar venta (descontar stock)",
			description = "Valida stock de todos los ítems, descuenta en una transacción y devuelve los precios vigentes. "
					+ "Idempotente por 'referencia': repetir la misma venta no descuenta dos veces.")
	@ApiResponse(responseCode = "201", description = "Venta registrada (o la misma ya registrada antes)")
	@ApiResponse(responseCode = "401", description = "Falta la clave de servicio o es incorrecta", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	@ApiResponse(responseCode = "404", description = "Producto inexistente", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	@ApiResponse(responseCode = "409", description = "Stock insuficiente o referencia usada con otros productos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public VentaResponse registrar(@Valid @RequestBody VentaRequest pedido) {
		return servicio.registrar(pedido);
	}

	@GetMapping("/inventario")
	@Operation(summary = "Resumen de inventario", description = "Unidades totales en stock, para el reporte de ventas.")
	public InventarioResponse inventario() {
		return servicio.inventario();
	}
}
