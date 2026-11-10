package cl.sonidovivo.pedidos.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.sonidovivo.pedidos.dto.ReporteVentasResponse;
import cl.sonidovivo.pedidos.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/reportes")
@Tag(name = "Reportes", description = "Métricas de ventas. Solo Administrador")
@SecurityRequirement(name = "bearerAuth")
public class ReportesController {

	private final ReporteService servicio;

	public ReportesController(ReporteService servicio) {
		this.servicio = servicio;
	}

	@GetMapping("/ventas")
	@Operation(summary = "Resumen de ventas", description = "Total vendido, ticket promedio, top 5 de productos y ventas por categoría.")
	public ReporteVentasResponse ventas() {
		return servicio.ventas();
	}
}
