package cl.sonidovivo.pedidos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import cl.sonidovivo.common.exception.ApiException;
import cl.sonidovivo.common.exception.ConflictoException;
import cl.sonidovivo.common.exception.RecursoNoEncontradoException;
import cl.sonidovivo.common.exception.ReglaNegocioException;
import cl.sonidovivo.common.exception.ServicioNoDisponibleException;
import cl.sonidovivo.pedidos.client.CatalogoCliente;
import cl.sonidovivo.pedidos.client.VentaCatalogo;
import cl.sonidovivo.pedidos.dto.BoletaRequest;
import cl.sonidovivo.pedidos.dto.BoletaResponse;
import cl.sonidovivo.pedidos.model.Boleta;
import cl.sonidovivo.pedidos.model.Cupon;
import cl.sonidovivo.pedidos.repository.BoletaRepository;

/**
 * Flujo de compra entre microservicios con catalogo SIMULADO (Mockito): validaciones previas,
 * rechazo por stock (409), reintento ante falla de red, 503 y la Idempotency-Key.
 */
@ExtendWith(MockitoExtension.class)
class BoletaServiceTest {

	@Mock
	BoletaRepository repo;
	@Mock
	BoletaStore store;
	@Mock
	CatalogoCliente catalogo;
	BoletaService servicio;

	@BeforeEach
	void preparar() {
		servicio = new BoletaService(repo, store, catalogo);
	}

	static BoletaRequest pedido(String cupon, BoletaRequest.Item... items) {
		return new BoletaRequest(
				new BoletaRequest.Cliente("Camila", "Rojas", "camila.rojas@gmail.com"),
				new BoletaRequest.Direccion("Av. Providencia 100", null, "Región Metropolitana de Santiago", "Providencia", null),
				List.of(items), cupon);
	}

	static BoletaRequest.Item item(String codigo, int cantidad) {
		return new BoletaRequest.Item(codigo, cantidad);
	}

	static VentaCatalogo.Respuesta venta(String ref) {
		return new VentaCatalogo.Respuesta(ref, Instant.now(),
				List.of(new VentaCatalogo.Linea("GA001", "Guitarra Folk", "Guitarras Acústicas", 3, 110_490, 331_470)), 331_470);
	}

	final BoletaResponse confirmada = mock(BoletaResponse.class);

	@Test
	void compraSinSesionAgrupaItemsYConfirmaConLosPreciosDeCatalogo() {
		when(store.crearPendiente(any(Boleta.class))).thenReturn(1001L);
		when(catalogo.registrarVenta(any())).thenReturn(venta("1001"));
		when(store.confirmar(eq(1001L), any(), eq(Optional.of(Cupon.SONIDOVIVO10)))).thenReturn(confirmada);

		var r = servicio.crear(pedido(" sonidovivo10 ", item("ga001", 1), item("GA001", 2)), Optional.empty(), null);

		assertThat(r).isSameAs(confirmada);
		verify(store).crearPendiente(argThat(b -> b.getUsuarioRun() == null && "SONIDOVIVO10".equals(b.getCupon())));
		verify(catalogo).registrarVenta(argThat(p -> p.referencia().equals("1001")
				&& p.items().equals(List.of(new VentaCatalogo.Item("GA001", 3)))));
	}

	@Test
	void conSesionGuardaElRunDelToken() {
		when(store.crearPendiente(any(Boleta.class))).thenReturn(1002L);
		when(catalogo.registrarVenta(any())).thenReturn(venta("1002"));

		servicio.crear(pedido(null, item("GA001", 1)), Optional.of("33333333-3"), null);

		verify(store).crearPendiente(argThat(b -> "33333333-3".equals(b.getUsuarioRun())));
	}

	@Test
	void cuponInvalidoOCarritoVacioSeRechazanAntesDeTocarNada() {
		assertThatThrownBy(() -> servicio.crear(pedido("NOEXISTE", item("GA001", 1)), Optional.empty(), null))
				.isInstanceOf(ReglaNegocioException.class).hasMessage("El cupón \"NOEXISTE\" no es válido.");
		assertThatThrownBy(() -> servicio.crear(pedido(null), Optional.empty(), null))
				.isInstanceOf(ReglaNegocioException.class).hasMessage("El carrito está vacío.");
		verifyNoInteractions(store, catalogo);
	}

	@Test
	void sinStockEnCatalogoBorraLaPendienteYDevuelve409() {
		when(store.crearPendiente(any(Boleta.class))).thenReturn(1003L);
		when(catalogo.registrarVenta(any()))
				.thenThrow(new ApiException(HttpStatus.CONFLICT, "No hay stock suficiente de \"Dreadnought\" (disponible: 1)."));

		assertThatThrownBy(() -> servicio.crear(pedido(null, item("GA002", 5)), Optional.empty(), null))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("No hay stock suficiente")
				.extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
		verify(store).eliminarPendiente(1003L);
		verify(store, never()).confirmar(any(), any(), any());
		verify(catalogo, times(1)).registrarVenta(any()); // un rechazo de negocio NO se reintenta
	}

	@Test
	void fallaDeRedSeReintentaUnaVezConLaMismaReferencia() {
		when(store.crearPendiente(any(Boleta.class))).thenReturn(1004L);
		when(catalogo.registrarVenta(any()))
				.thenThrow(new ServicioNoDisponibleException("caído"))
				.thenReturn(venta("1004"));

		servicio.crear(pedido(null, item("GA001", 1)), Optional.empty(), null);

		verify(catalogo, times(2)).registrarVenta(argThat(p -> p.referencia().equals("1004")));
		verify(store).confirmar(eq(1004L), any(), any());
	}

	@Test
	void siCatalogoSigueCaidoResponde503YLaBoletaQuedaPendiente() {
		when(store.crearPendiente(any(Boleta.class))).thenReturn(1005L);
		when(catalogo.registrarVenta(any())).thenThrow(new ServicioNoDisponibleException("caído"));

		assertThatThrownBy(() -> servicio.crear(pedido(null, item("GA001", 1)), Optional.empty(), "clave-1"))
				.isInstanceOf(ServicioNoDisponibleException.class).hasMessageContaining("no se cobrará dos veces");
		verify(catalogo, times(2)).registrarVenta(any());
		verify(store, never()).eliminarPendiente(any()); // pudo haberse descontado: no se borra
	}

	@Test
	void mismaClaveYMismaCompraDevuelveLaBoletaSinVolverACobrar() {
		BoletaRequest p = pedido(null, item("GA001", 1));
		Boleta previa = new Boleta();
		previa.setId(1006L);
		previa.setEstado(Boleta.ESTADO_PAGADA);
		previa.setHuella(BoletaService.huella(p, BoletaService.agrupar(p.items()), Optional.empty()));
		when(store.buscarPorClave("clave-2")).thenReturn(Optional.of(previa));
		when(store.respuesta(1006L)).thenReturn(confirmada);

		assertThat(servicio.crear(p, Optional.empty(), "clave-2")).isSameAs(confirmada);
		verifyNoInteractions(catalogo);
		verify(store, never()).crearPendiente(any());
	}

	@Test
	void mismaClaveConOtraCompraEsConflicto() {
		Boleta previa = new Boleta();
		previa.setId(1007L);
		previa.setHuella("otra-huella");
		when(store.buscarPorClave("clave-3")).thenReturn(Optional.of(previa));

		assertThatThrownBy(() -> servicio.crear(pedido(null, item("GA001", 2)), Optional.empty(), "clave-3"))
				.isInstanceOf(ConflictoException.class).hasMessageContaining("Idempotency-Key");
	}

	@Test
	void unaPendienteConLaMismaClaveSeRetomaConSuNumero() {
		BoletaRequest p = pedido(null, item("GA001", 1));
		Boleta previa = new Boleta();
		previa.setId(1008L);
		previa.setEstado(Boleta.ESTADO_PENDIENTE);
		previa.setHuella(BoletaService.huella(p, BoletaService.agrupar(p.items()), Optional.empty()));
		when(store.buscarPorClave("clave-4")).thenReturn(Optional.of(previa));
		when(catalogo.registrarVenta(any())).thenReturn(venta("1008"));

		servicio.crear(p, Optional.empty(), "clave-4");

		verify(store, never()).crearPendiente(any());
		verify(catalogo).registrarVenta(argThat(x -> x.referencia().equals("1008")));
	}

	@Test
	void unClienteSoloVeSusBoletasYElPersonalTodas() {
		Boleta b = new Boleta();
		b.setId(1009L);
		b.setUsuarioRun("33333333-3");
		b.setEstado(Boleta.ESTADO_PAGADA);
		when(repo.findByIdAndEstado(1009L, Boleta.ESTADO_PAGADA)).thenReturn(Optional.of(b));

		assertThat(servicio.obtener(1009L, "33333333-3", false).id()).isEqualTo(1009L);
		assertThat(servicio.obtener(1009L, "22222222-2", true).id()).isEqualTo(1009L);
		assertThatThrownBy(() -> servicio.obtener(1009L, "44444444-4", false)).isInstanceOf(AccessDeniedException.class);
		assertThatThrownBy(() -> servicio.obtener(1L, "33333333-3", false)).isInstanceOf(RecursoNoEncontradoException.class);
	}

	@Test
	void cuponesConRedondeo() {
		assertThat(Cupon.buscar(" bienvenido5 ")).contains(Cupon.BIENVENIDO5);
		assertThat(Cupon.buscar("X")).isEmpty();
		assertThat(Cupon.SONIDOVIVO10.descuentoSobre(12_345)).isEqualTo(1_235);
	}
}
