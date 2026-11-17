package cl.sonidovivo.pedidos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.sonidovivo.pedidos.client.CatalogoCliente;
import cl.sonidovivo.pedidos.dto.ReporteVentasResponse;
import cl.sonidovivo.pedidos.dto.ReporteVentasResponse.ProductoVendido;
import cl.sonidovivo.pedidos.dto.ReporteVentasResponse.VentaCategoria;
import cl.sonidovivo.pedidos.model.Boleta;
import cl.sonidovivo.pedidos.model.DetalleBoleta;
import cl.sonidovivo.pedidos.repository.BoletaRepository;

/**
 * Reporte de ventas: totales, ticket promedio, top 5 por unidades, monto por categoría
 * y el stock de catalogo (null si catalogo no responde).
 */
@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

	@Mock
	BoletaRepository repo;
	@Mock
	CatalogoCliente catalogo;
	ReporteService servicio;

	@BeforeEach
	void preparar() {
		servicio = new ReporteService(repo, catalogo);
	}

	static Boleta boleta(DetalleBoleta... detalles) {
		Boleta b = new Boleta();
		b.setEstado(Boleta.ESTADO_PAGADA);
		int total = 0;
		for (DetalleBoleta d : detalles) {
			b.agregarDetalle(d);
			total += d.getSubtotal();
		}
		b.setTotal(total);
		return b;
	}

	static DetalleBoleta detalle(String codigo, String categoria, int cantidad, int precio) {
		return new DetalleBoleta(codigo, "Producto " + codigo, categoria, cantidad, precio);
	}

	@Test
	void sinVentasDevuelveCerosYTicketPromedioCero() {
		when(repo.findByEstadoOrderByFechaDescIdDesc(Boleta.ESTADO_PAGADA)).thenReturn(List.of());
		when(catalogo.unidadesEnStock()).thenReturn(Optional.of(230L));

		ReporteVentasResponse r = servicio.ventas();

		assertThat(r.cantidadOrdenes()).isZero();
		assertThat(r.totalVendido()).isZero();
		assertThat(r.ticketPromedio()).isZero();
		assertThat(r.unidadesEnStock()).isEqualTo(230L);
		assertThat(r.topProductos()).isEmpty();
		assertThat(r.porCategoria()).isEmpty();
	}

	@Test
	void acumulaPorProductoYCategoriaYOrdenaDeMayorAMenor() {
		when(repo.findByEstadoOrderByFechaDescIdDesc(Boleta.ESTADO_PAGADA)).thenReturn(List.of(
				boleta(detalle("GA001", "Guitarras", 1, 100_000), detalle("BA001", "Baterías", 1, 300_000)),
				boleta(detalle("GA001", "Guitarras", 2, 100_000), detalle("CU001", "Cuerdas", 5, 5_000))));
		when(catalogo.unidadesEnStock()).thenReturn(Optional.of(50L));

		ReporteVentasResponse r = servicio.ventas();

		assertThat(r.cantidadOrdenes()).isEqualTo(2);
		assertThat(r.totalVendido()).isEqualTo(625_000);
		assertThat(r.ticketPromedio()).isEqualTo(312_500);
		assertThat(r.topProductos()).containsExactly(
				new ProductoVendido("CU001", "Producto CU001", 5, 25_000),
				new ProductoVendido("GA001", "Producto GA001", 3, 300_000),
				new ProductoVendido("BA001", "Producto BA001", 1, 300_000));
		assertThat(r.porCategoria()).containsExactly(
				new VentaCategoria("Guitarras", 300_000),
				new VentaCategoria("Baterías", 300_000),
				new VentaCategoria("Cuerdas", 25_000));
	}

	@Test
	void topLimitaACincoProductosYStockQuedaNullSiCatalogoNoResponde() {
		when(repo.findByEstadoOrderByFechaDescIdDesc(Boleta.ESTADO_PAGADA)).thenReturn(List.of(boleta(
				detalle("P1", "A", 1, 1_000), detalle("P2", "A", 2, 1_000), detalle("P3", "A", 3, 1_000),
				detalle("P4", "A", 4, 1_000), detalle("P5", "A", 5, 1_000), detalle("P6", "A", 6, 1_000))));
		when(catalogo.unidadesEnStock()).thenReturn(Optional.empty());

		ReporteVentasResponse r = servicio.ventas();

		assertThat(r.unidadesEnStock()).isNull();
		assertThat(r.topProductos()).extracting(ProductoVendido::codigo)
				.containsExactly("P6", "P5", "P4", "P3", "P2");
	}
}
