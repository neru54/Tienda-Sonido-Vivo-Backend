package cl.sonidovivo.pedidos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.sonidovivo.common.exception.ErrorRespuesta;
import cl.sonidovivo.common.security.SesionActual;
import cl.sonidovivo.pedidos.dto.BoletaRequest;
import cl.sonidovivo.pedidos.dto.BoletaResponse;
import cl.sonidovivo.pedidos.service.BoletaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/boletas")
@Tag(name = "Boletas", description = "Checkout (público) y consulta de órdenes")
public class BoletasController {

	private final BoletaService servicio;
	private final SesionActual sesion;

	public BoletasController(BoletaService servicio, SesionActual sesion) {
		this.servicio = servicio;
		this.sesion = sesion;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Comprar (crear boleta)",
			description = "Público: se puede comprar sin cuenta. Si se envía un token, la boleta queda asociada al RUN del usuario. "
					+ "Valida el cupón, pide a catálogo los precios reales y el descuento de stock (todo o nada) y calcula el total.",
			security = { @SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "") })
	@ApiResponse(responseCode = "201", description = "Compra realizada")
	@ApiResponse(responseCode = "400", description = "Datos inválidos o cupón no válido", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	@ApiResponse(responseCode = "404", description = "Producto inexistente", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	@ApiResponse(responseCode = "409", description = "Stock insuficiente o Idempotency-Key usada con otra compra", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	@ApiResponse(responseCode = "503", description = "Catálogo no disponible (reintentar con la misma Idempotency-Key)", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public BoletaResponse crear(@Valid @RequestBody BoletaRequest pedido,
			@Parameter(description = "Opcional. UUID por intento de compra: repetir la petición con la misma clave no crea otra boleta")
			@RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
		return servicio.crear(pedido, sesion.run(), idempotencyKey);
	}

	@GetMapping
	@Operation(summary = "Listar boletas (órdenes)", description = "Más recientes primero. Administrador y Vendedor.",
			security = @SecurityRequirement(name = "bearerAuth"))
	public List<BoletaResponse> listar() {
		return servicio.listar();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Detalle de una boleta", description = "Administrador y Vendedor ven cualquiera; un cliente solo las suyas.",
			security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "403", description = "La boleta es de otra persona", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	@ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public BoletaResponse obtener(@PathVariable Long id) {
		return servicio.obtener(id, sesion.run().orElse(null), sesion.esPersonal());
	}
}
