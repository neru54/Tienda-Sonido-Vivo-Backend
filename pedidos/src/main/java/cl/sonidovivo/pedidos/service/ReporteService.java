package cl.sonidovivo.pedidos.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.sonidovivo.pedidos.client.CatalogoCliente;
import cl.sonidovivo.pedidos.dto.ReporteVentasResponse;
import cl.sonidovivo.pedidos.dto.ReporteVentasResponse.ProductoVendido;
import cl.sonidovivo.pedidos.dto.ReporteVentasResponse.VentaCategoria;
import cl.sonidovivo.pedidos.model.Boleta;
import cl.sonidovivo.pedidos.model.DetalleBoleta;
import cl.sonidovivo.pedidos.repository.BoletaRepository;

/**
 * Reportes de ventas sobre las boletas PAGADAS.
 * Nombre y categoría salen del propio detalle (guardados al vender), así que solo
 * "unidadesEnStock" requiere preguntarle a catalogo.
 */
@Service
@Transactional(readOnly = true)
public class ReporteService {

	private final BoletaRepository boletas;
	private final CatalogoCliente catalogo;

	public ReporteService(BoletaRepository boletas, CatalogoCliente catalogo) {
		this.boletas = boletas;
		this.catalogo = catalogo;
	}

	public ReporteVentasResponse ventas() {
		List<Boleta> todas = boletas.findByEstadoOrderByFechaDescIdDesc(Boleta.ESTADO_PAGADA);
		long totalVendido = todas.stream().mapToLong(Boleta::getTotal).sum();

		Map<String, long[]> porProducto = new LinkedHashMap<>(); // codigo -> [unidades, monto]
		Map<String, String> nombres = new LinkedHashMap<>();
		Map<String, Long> porCategoria = new LinkedHashMap<>();
		for (Boleta b : todas) {
			for (DetalleBoleta d : b.getDetalles()) {
				long[] acumulado = porProducto.computeIfAbsent(d.getCodigoProducto(), k -> new long[2]);
				acumulado[0] += d.getCantidad();
				acumulado[1] += d.getSubtotal();
				nombres.putIfAbsent(d.getCodigoProducto(), d.getNombreProducto());
				porCategoria.merge(d.getCategoriaProducto(), (long) d.getSubtotal(), Long::sum);
			}
		}

		List<ProductoVendido> top = porProducto.entrySet().stream()
				.map(e -> new ProductoVendido(e.getKey(), nombres.get(e.getKey()), e.getValue()[0], e.getValue()[1]))
				.sorted(Comparator.comparingLong(ProductoVendido::unidades).reversed())
				.limit(5)
				.toList();
		List<VentaCategoria> categorias = porCategoria.entrySet().stream()
				.map(e -> new VentaCategoria(e.getKey(), e.getValue()))
				.sorted(Comparator.comparingLong(VentaCategoria::monto).reversed())
				.toList();

		return new ReporteVentasResponse(todas.size(), totalVendido,
				todas.isEmpty() ? 0 : Math.round((double) totalVendido / todas.size()),
				catalogo.unidadesEnStock().orElse(null), top, categorias);
	}
}
