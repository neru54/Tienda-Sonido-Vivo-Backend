package cl.sonidovivo.pedidos.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.sonidovivo.common.exception.ApiException;
import cl.sonidovivo.common.exception.ConflictoException;
import cl.sonidovivo.common.exception.RecursoNoEncontradoException;
import cl.sonidovivo.common.exception.ReglaNegocioException;
import cl.sonidovivo.common.exception.ServicioNoDisponibleException;
import cl.sonidovivo.common.util.Textos;
import cl.sonidovivo.pedidos.client.CatalogoCliente;
import cl.sonidovivo.pedidos.client.VentaCatalogo;
import cl.sonidovivo.pedidos.dto.BoletaRequest;
import cl.sonidovivo.pedidos.dto.BoletaResponse;
import cl.sonidovivo.pedidos.mapper.BoletaMapper;
import cl.sonidovivo.pedidos.model.Boleta;
import cl.sonidovivo.pedidos.model.Cupon;
import cl.sonidovivo.pedidos.repository.BoletaRepository;

/**
 * Checkout y consulta de boletas. El stock está en otro
 * servicio, así que la compra es una secuencia de pasos (sin transacción distribuida):
 *
 *  1. Se valida el carrito y el cupón (400) ANTES de tocar nada.
 *  2. Idempotency-Key opcional: si ya existe una boleta con esa clave y el mismo contenido, se devuelve
 *     (o se retoma si quedó pendiente). Misma clave con otro contenido -> 409.
 *  3. Se guarda la boleta PENDIENTE (obtiene su número, desde 1001).
 *  4. Se llama a catalogo /internal/ventas con referencia = número de boleta. Catalogo valida y
 *     descuenta el stock de todos los ítems en su transacción y devuelve los precios reales.
 *       - 409/404/400 (sin stock, producto inexistente): no se descontó nada -> se borra la pendiente.
 *       - Sin respuesta (red, caída): se reintenta UNA vez con la misma referencia; catalogo es idempotente,
 *         así que nunca descuenta dos veces. Si vuelve a fallar -> 503 y la boleta queda PENDIENTE.
 *  5. Se guarda el detalle, se aplica el cupón y la boleta pasa a PAGADA.
 *
 * Límite (documentado): si catalogo descontó y la respuesta se perdió dos veces, la boleta queda
 * pendiente con el stock ya descontado. Repetir la compra con la MISMA Idempotency-Key la completa;
 * sin clave, queda para revisión manual. No hay un proceso automático de conciliación.
 */
@Service
@Transactional(readOnly = true)
public class BoletaService {

	private static final Logger log = LoggerFactory.getLogger(BoletaService.class);

	private final BoletaRepository boletas;
	private final BoletaStore store;
	private final CatalogoCliente catalogo;

	public BoletaService(BoletaRepository boletas, BoletaStore store, CatalogoCliente catalogo) {
		this.boletas = boletas;
		this.store = store;
		this.catalogo = catalogo;
	}

	/**
	 * NO es @Transactional (propagation NOT_SUPPORTED): cada paso de base de datos ocurre en BoletaStore,
	 * y la llamada HTTP a catalogo se hace sin una transacción abierta.
	 */
	@Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
	public BoletaResponse crear(BoletaRequest pedido, Optional<String> runSesion, String idempotencyKey) {
		Map<String, Integer> cantidades = agrupar(pedido.items());

		Optional<Cupon> cupon = Optional.empty();
		if (pedido.cupon() != null && !pedido.cupon().isBlank()) {
			cupon = Optional.of(Cupon.buscar(pedido.cupon())
					.orElseThrow(() -> new ReglaNegocioException("El cupón \"" + pedido.cupon().trim() + "\" no es válido.")));
		}

		String clave = Textos.limpio(idempotencyKey);
		if (clave != null && clave.length() > 64) throw new ReglaNegocioException("La clave Idempotency-Key es demasiado larga (máximo 64).");
		String huella = huella(pedido, cantidades, runSesion);

		Long id = null;
		if (clave != null) {
			Optional<Boleta> previa = store.buscarPorClave(clave);
			if (previa.isPresent()) {
				Boleta b = previa.get();
				if (!huella.equals(b.getHuella())) {
					throw new ConflictoException("La clave Idempotency-Key ya se usó con otra compra.");
				}
				if (b.estaPagada()) return store.respuesta(b.getId()); // reintento de una compra ya hecha
				id = b.getId(); // quedó pendiente: se retoma con la misma referencia
			}
		}
		if (id == null) {
			id = store.crearPendiente(nuevaBoleta(pedido, runSesion, cupon, clave, huella));
		}

		VentaCatalogo.Respuesta venta = registrarEnCatalogo(id, cantidades);
		return store.confirmar(id, venta, cupon);
	}

	/** Llamada a catalogo con un reintento ante fallas de red; los rechazos de negocio no se reintentan. */
	private VentaCatalogo.Respuesta registrarEnCatalogo(Long id, Map<String, Integer> cantidades) {
		List<VentaCatalogo.Item> items = cantidades.entrySet().stream()
				.map(e -> new VentaCatalogo.Item(e.getKey(), e.getValue())).toList();
		VentaCatalogo.Peticion peticion = new VentaCatalogo.Peticion(String.valueOf(id), items);
		for (int intento = 1; ; intento++) {
			try {
				return catalogo.registrarVenta(peticion);
			} catch (ServicioNoDisponibleException e) {
				if (intento >= 2) {
					log.warn("Boleta {} queda PENDIENTE: catálogo no respondió tras {} intentos", id, intento);
					throw new ServicioNoDisponibleException(
							"No pudimos confirmar tu compra con el catálogo. Inténtalo nuevamente en unos segundos; no se cobrará dos veces.");
				}
				log.info("Reintentando la venta de la boleta {} en catálogo", id);
			} catch (ApiException e) {
				store.eliminarPendiente(id); // rechazo definitivo: catalogo no descontó nada
				throw e;
			}
		}
	}

	public List<BoletaResponse> listar() {
		return boletas.findByEstadoOrderByFechaDescIdDesc(Boleta.ESTADO_PAGADA).stream().map(BoletaMapper::aRespuesta).toList();
	}

	/** Historial de un RUN. Si no hay compras devuelve []: pedidos no consulta el esquema de usuarios. */
	public List<BoletaResponse> deUsuario(String run) {
		return boletas.findByUsuarioRunAndEstadoOrderByFechaDescIdDesc(Textos.normalizarRun(run), Boleta.ESTADO_PAGADA)
				.stream().map(BoletaMapper::aRespuesta).toList();
	}

	/** El personal ve cualquier boleta; un cliente solo las suyas (si no, 403: no basta con adivinar el número). */
	public BoletaResponse obtener(Long id, String runSesion, boolean esPersonal) {
		Boleta b = boletas.findByIdAndEstado(id, Boleta.ESTADO_PAGADA)
				.orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la orden."));
		boolean esDueno = b.getUsuarioRun() != null && b.getUsuarioRun().equals(runSesion);
		if (!esPersonal && !esDueno) throw new AccessDeniedException("La boleta no pertenece al usuario.");
		return BoletaMapper.aRespuesta(b);
	}

	private static Boleta nuevaBoleta(BoletaRequest pedido, Optional<String> run, Optional<Cupon> cupon, String clave, String huella) {
		Boleta b = new Boleta();
		b.setUsuarioRun(run.orElse(null));
		b.setNombreCliente(pedido.cliente().nombre().trim());
		b.setApellidosCliente(pedido.cliente().apellidos().trim());
		b.setCorreoCliente(pedido.cliente().correo().trim());
		b.setCalle(pedido.direccion().calle().trim());
		b.setDepartamento(Textos.limpio(pedido.direccion().departamento()));
		b.setRegion(pedido.direccion().region());
		b.setComuna(pedido.direccion().comuna());
		b.setIndicaciones(Textos.limpio(pedido.direccion().indicaciones()));
		b.setCupon(cupon.map(Cupon::name).orElse(null));
		b.setIdempotencyKey(clave);
		b.setHuella(huella);
		return b;
	}

	/** Suma las cantidades de un mismo producto aunque venga en varias líneas, manteniendo el orden. */
	static Map<String, Integer> agrupar(List<BoletaRequest.Item> items) {
		if (items == null || items.isEmpty()) throw new ReglaNegocioException("El carrito está vacío.");
		Map<String, Integer> resultado = new LinkedHashMap<>();
		for (BoletaRequest.Item i : items) {
			resultado.merge(Textos.normalizarCodigo(i.codigo()), i.cantidad(), Integer::sum);
		}
		return resultado;
	}

	/** SHA-256 de lo esencial de la compra (quién, qué y con qué cupón). */
	static String huella(BoletaRequest pedido, Map<String, Integer> cantidades, Optional<String> run) {
		String texto = run.orElse("-") + "|" + Textos.recortado(pedido.cliente().correo()).toLowerCase()
				+ "|" + new TreeMap<>(cantidades) + "|" + Textos.recortado(pedido.cupon()).toUpperCase();
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
