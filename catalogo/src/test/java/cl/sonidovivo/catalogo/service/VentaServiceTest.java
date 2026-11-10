package cl.sonidovivo.catalogo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.sonidovivo.catalogo.dto.VentaRequest;
import cl.sonidovivo.catalogo.dto.VentaRequest.Item;
import cl.sonidovivo.catalogo.model.Categoria;
import cl.sonidovivo.catalogo.model.Producto;
import cl.sonidovivo.catalogo.model.VentaLinea;
import cl.sonidovivo.catalogo.model.VentaRegistrada;
import cl.sonidovivo.catalogo.repository.ProductoRepository;
import cl.sonidovivo.catalogo.repository.VentaRegistradaRepository;
import cl.sonidovivo.common.exception.ConflictoException;
import cl.sonidovivo.common.exception.RecursoNoEncontradoException;

/** Descuento de stock de una compra: precio vigente, todo-o-nada, bloqueo ordenado e idempotencia. */
@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

	@Mock
	ProductoRepository productos;
	@Mock
	VentaRegistradaRepository ventas;
	VentaService servicio;

	final Categoria guitarras = new Categoria("guitarras-acusticas", "Guitarras Acústicas", null, null);

	@BeforeEach
	void preparar() {
		servicio = new VentaService(productos, ventas, Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"), ZoneOffset.UTC));
	}

	static Producto producto(String codigo, String nombre, Categoria c, int precio, int stock, boolean oferta, Integer precioOferta) {
		Producto p = new Producto();
		p.setCodigo(codigo);
		p.setNombre(nombre);
		p.setCategoria(c);
		p.setPrecio(precio);
		p.setStock(stock);
		p.setOferta(oferta);
		p.setPrecioOferta(precioOferta);
		return p;
	}

	@Test
	void cobraPrecioVigenteAgrupaRepetidosYDescuentaStock() {
		Producto ga001 = producto("GA001", "Guitarra Folk", guitarras, 129_990, 8, true, 110_490);
		Producto ga002 = producto("GA002", "Dreadnought", guitarras, 189_990, 5, false, null);
		when(productos.findByCodigoParaActualizar("GA001")).thenReturn(Optional.of(ga001));
		when(productos.findByCodigoParaActualizar("GA002")).thenReturn(Optional.of(ga002));
		when(ventas.save(any(VentaRegistrada.class))).thenAnswer(i -> i.getArgument(0));

		var r = servicio.registrar(new VentaRequest("1001",
				List.of(new Item("ga001", 1), new Item("GA002", 1), new Item("GA001 ", 2))));

		assertThat(r.referencia()).isEqualTo("1001");
		assertThat(r.lineas()).hasSize(2);
		assertThat(r.lineas().get(0).codigo()).isEqualTo("GA001");
		assertThat(r.lineas().get(0).cantidad()).isEqualTo(3);
		assertThat(r.lineas().get(0).precioUnitario()).isEqualTo(110_490); // de oferta
		assertThat(r.lineas().get(0).categoria()).isEqualTo("Guitarras Acústicas");
		assertThat(r.subtotal()).isEqualTo(3 * 110_490 + 189_990);
		assertThat(ga001.getStock()).isEqualTo(5);
		assertThat(ga002.getStock()).isEqualTo(4);
	}

	@Test
	void siUnoNoTieneStockNoSeDescuentaNinguno() {
		Producto ga001 = producto("GA001", "Guitarra Folk", guitarras, 129_990, 8, false, null);
		Producto ga002 = producto("GA002", "Dreadnought", guitarras, 189_990, 1, false, null);
		when(productos.findByCodigoParaActualizar("GA001")).thenReturn(Optional.of(ga001));
		when(productos.findByCodigoParaActualizar("GA002")).thenReturn(Optional.of(ga002));

		assertThatThrownBy(() -> servicio.registrar(new VentaRequest("1002", List.of(new Item("GA001", 2), new Item("GA002", 3)))))
				.isInstanceOf(ConflictoException.class)
				.hasMessage("No hay stock suficiente de \"Dreadnought\" (disponible: 1).");

		assertThat(ga001.getStock()).isEqualTo(8);
		assertThat(ga002.getStock()).isEqualTo(1);
		verify(ventas, never()).save(any());
	}

	@Test
	void productoInexistenteDa404() {
		when(productos.findByCodigoParaActualizar("ZZ999")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> servicio.registrar(new VentaRequest("1003", List.of(new Item("ZZ999", 1)))))
				.isInstanceOf(RecursoNoEncontradoException.class)
				.hasMessage("El producto ZZ999 ya no existe.");
	}

	@Test
	void bloqueaLosProductosEnOrdenDeCodigoParaEvitarDeadlocks() {
		Producto b = producto("BA001", "Batería", guitarras, 1000, 5, false, null);
		Producto g = producto("GA001", "Guitarra", guitarras, 1000, 5, false, null);
		Producto t = producto("TE001", "Teclado", guitarras, 1000, 5, false, null);
		when(productos.findByCodigoParaActualizar(anyString())).thenAnswer(i -> Optional.of(
				switch ((String) i.getArgument(0)) { case "BA001" -> b; case "GA001" -> g; default -> t; }));
		when(ventas.save(any(VentaRegistrada.class))).thenAnswer(i -> i.getArgument(0));

		servicio.registrar(new VentaRequest("1004", List.of(new Item("TE001", 1), new Item("BA001", 1), new Item("GA001", 1))));

		InOrder orden = inOrder(productos);
		orden.verify(productos).findByCodigoParaActualizar("BA001");
		orden.verify(productos).findByCodigoParaActualizar("GA001");
		orden.verify(productos).findByCodigoParaActualizar("TE001");
	}

	@Test
	void reintentoConLaMismaReferenciaNoDescuentaDosVeces() {
		VentaRegistrada previa = new VentaRegistrada("1005", Instant.parse("2026-11-10T14:00:00Z"));
		previa.getLineas().add(new VentaLinea("GA001", "Guitarra Folk", "Guitarras Acústicas", 2, 110_490));
		when(ventas.findById("1005")).thenReturn(Optional.of(previa));

		var r = servicio.registrar(new VentaRequest("1005", List.of(new Item("GA001", 2))));

		assertThat(r.subtotal()).isEqualTo(220_980);
		verify(productos, never()).findByCodigoParaActualizar(anyString());
		verify(ventas, never()).save(any());
	}

	@Test
	void mismaReferenciaConOtrosProductosEsConflicto() {
		VentaRegistrada previa = new VentaRegistrada("1006", Instant.now());
		previa.getLineas().add(new VentaLinea("GA001", "Guitarra Folk", "Guitarras Acústicas", 2, 110_490));
		when(ventas.findById("1006")).thenReturn(Optional.of(previa));

		assertThatThrownBy(() -> servicio.registrar(new VentaRequest("1006", List.of(new Item("GA001", 3)))))
				.isInstanceOf(ConflictoException.class).hasMessageContaining("ya se usó con otros productos");
	}

	@Test
	void inventarioSumaElStock() {
		when(productos.sumarStock()).thenReturn(230L);
		when(productos.count()).thenReturn(51L);
		assertThat(servicio.inventario().unidadesEnStock()).isEqualTo(230L);
		assertThat(servicio.inventario().productos()).isEqualTo(51L);
	}
}
