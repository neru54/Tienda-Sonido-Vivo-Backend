package cl.sonidovivo.pedidos.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.sonidovivo.common.security.UsuarioToken;
import cl.sonidovivo.pedidos.dto.BoletaResponse;
import cl.sonidovivo.pedidos.service.BoletaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Historial de compras. Las rutas están bajo /usuarios porque así las usa el frontend,
 * pero los DATOS son boletas: el Gateway envía estas dos rutas a pedidos y el resto de
 * /usuarios/** al microservicio usuarios. Así no se duplica información entre servicios.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Historial de compras", description = "Boletas por usuario (rutas /usuarios atendidas por pedidos)")
@SecurityRequirement(name = "bearerAuth")
public class HistorialController {

	private final BoletaService servicio;

	public HistorialController(BoletaService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/me/boletas")
	@Operation(summary = "Mis compras", description = "Boletas asociadas al RUN del token. Cliente o Administrador.")
	public List<BoletaResponse> misBoletas(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioToken yo) {
		return servicio.deUsuario(yo.run());
	}

	@GetMapping("/{run}/boletas")
	@Operation(summary = "Historial de compras de un usuario", description = "Solo Administrador. Lista vacía si el RUN no tiene compras.")
	public List<BoletaResponse> historial(@PathVariable String run) {
		return servicio.deUsuario(run);
	}
}
