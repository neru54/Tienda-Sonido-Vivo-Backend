package cl.sonidovivo.catalogo.service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.sonidovivo.catalogo.dto.InventarioResponse;
import cl.sonidovivo.catalogo.dto.VentaRequest;
import cl.sonidovivo.catalogo.dto.VentaResponse;
import cl.sonidovivo.catalogo.model.Producto;
import cl.sonidovivo.catalogo.model.VentaLinea;
import cl.sonidovivo.catalogo.model.VentaRegistrada;
import cl.sonidovivo.catalogo.repository.ProductoRepository;
import cl.sonidovivo.catalogo.repository.VentaRegistradaRepository;
import cl.sonidovivo.common.exception.ConflictoException;
import cl.sonidovivo.common.exception.RecursoNoEncontradoException;
import cl.sonidovivo.common.util.Textos;

/**
 * Descuento de stock de una compra (lo llama SOLO pedidos, por /internal/ventas).
 * Es la parte "catálogo" del checkout, en UNA transacción local de este esquema:
 *
 *  0. Idempotencia: si la referencia (n.º de boleta) ya se registró, se devuelve ese comprobante y
 *     NO se descuenta de nuevo. Si llega la misma referencia con otros productos -> 409.
 *  1. Se agrupan los ítems repetidos (GA001 x1 + GA001 x2 = GA001 x3).
 *  2. Se bloquea cada producto (SELECT ... FOR UPDATE) en ORDEN DE CÓDIGO: dos compras simultáneas
 *     toman los bloqueos en el mismo orden y no pueden quedar esperándose mutuamente (deadlock).
 *  3. Se valida el stock de TODOS los productos ANTES de descontar: si uno falla, 409 y no se toca nada.
 *  4. Precio = el vigente en la base (el de oferta si corresponde), nunca el del navegador.
 *  5. Se descuenta y se guarda el comprobante con nombre, categoría y precio de ese momento.
 */
@Service
@Transactional(readOnly = true)
public class VentaService {

	private final ProductoRepository productos;
	private final VentaRegistradaRepository ventas;
	private final Clock reloj;

	public VentaService(ProductoRepository productos, VentaRegistradaRepository ventas, Clock reloj) {
		this.productos = productos;
		this.ventas = ventas;
		this.reloj = reloj;
	}

	@Transactional
	public VentaResponse registrar(VentaRequest pedido) {
		String referencia = pedido.referencia().trim();
		Map<String, Integer> cantidades = agrupar(pedido.items());

		Optional<VentaRegistrada> previa = ventas.findById(referencia);
		if (previa.isPresent()) {
			if (!mismosItems(previa.get(), cantidades)) {
				throw new ConflictoException("La referencia " + referencia + " ya se usó con otros productos.");
			}
			return VentaResponse.desde(previa.get()); // reintento: mismo resultado, sin descontar otra vez
		}

		// 2. Bloqueo en orden de código (TreeMap ordena las claves)
		Map<String, Producto> bloqueados = new TreeMap<>();
		for (String codigo : new TreeMap<>(cantidades).keySet()) {
			bloqueados.put(codigo, productos.findByCodigoParaActualizar(codigo)
					.orElseThrow(() -> new RecursoNoEncontradoException("El producto " + codigo + " ya no existe.")));
		}

		// 3. Validar todo antes de tocar nada (en el orden del carrito, para el mensaje)
		for (Map.Entry<String, Integer> item : cantidades.entrySet()) {
			Producto p = bloqueados.get(item.getKey());
			if (p.getStock() < item.getValue()) {
				throw new ConflictoException("No hay stock suficiente de \"" + p.getNombre() + "\" (disponible: " + p.getStock() + ").");
			}
		}

		// 4 y 5. Descontar y guardar el comprobante
		VentaRegistrada venta = new VentaRegistrada(referencia, Instant.now(reloj));
		for (Map.Entry<String, Integer> item : cantidades.entrySet()) {
			Producto p = bloqueados.get(item.getKey());
			int cantidad = item.getValue();
			p.setStock(p.getStock() - cantidad);
			venta.getLineas().add(new VentaLinea(p.getCodigo(), p.getNombre(), p.getCategoria().getNombre(), cantidad, p.precioVigente()));
		}
		return VentaResponse.desde(ventas.save(venta));
	}

	public InventarioResponse inventario() {
		return new InventarioResponse(productos.sumarStock(), productos.count());
	}

	/** Suma las cantidades de un mismo producto aunque venga en varias líneas, manteniendo el orden. */
	static Map<String, Integer> agrupar(List<VentaRequest.Item> items) {
		Map<String, Integer> resultado = new LinkedHashMap<>();
		for (VentaRequest.Item i : items) {
			resultado.merge(Textos.normalizarCodigo(i.codigo()), i.cantidad(), Integer::sum);
		}
		return resultado;
	}

	private static boolean mismosItems(VentaRegistrada venta, Map<String, Integer> cantidades) {
		Map<String, Integer> guardados = new LinkedHashMap<>();
		venta.getLineas().forEach(l -> guardados.put(l.getCodigo(), l.getCantidad()));
		return guardados.equals(cantidades);
	}
}
