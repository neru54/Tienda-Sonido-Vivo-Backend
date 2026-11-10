package cl.sonidovivo.pedidos.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.sonidovivo.pedidos.client.VentaCatalogo;
import cl.sonidovivo.pedidos.dto.BoletaResponse;
import cl.sonidovivo.pedidos.mapper.BoletaMapper;
import cl.sonidovivo.pedidos.model.Boleta;
import cl.sonidovivo.pedidos.model.Cupon;
import cl.sonidovivo.pedidos.model.DetalleBoleta;
import cl.sonidovivo.pedidos.repository.BoletaRepository;

/**
 * Pasos de base de datos de la compra, cada uno en su PROPIA transacción corta.
 *
 * ¿Por qué separado de BoletaService? Porque entre "crear pendiente" y "confirmar" hay una llamada
 * HTTP a catalogo. Si todo fuera una sola transacción, la conexión a MySQL quedaría abierta mientras
 * se espera la red, y si catalogo descuenta stock pero aquí se hace rollback, la boleta desaparecería.
 * Así, la boleta PENDIENTE ya está guardada antes de llamar y sirve de referencia para reintentar.
 */
@Service
public class BoletaStore {

	private final BoletaRepository boletas;
	private final Clock reloj;

	public BoletaStore(BoletaRepository boletas, Clock reloj) {
		this.boletas = boletas;
		this.reloj = reloj;
	}

	@Transactional
	public Long crearPendiente(Boleta boleta) {
		boleta.setEstado(Boleta.ESTADO_PENDIENTE);
		boleta.setFecha(Instant.now(reloj));
		return boletas.save(boleta).getId();
	}

	@Transactional(readOnly = true)
	public Optional<Boleta> buscarPorClave(String idempotencyKey) {
		return boletas.findByIdempotencyKey(idempotencyKey);
	}

	@Transactional(readOnly = true)
	public BoletaResponse respuesta(Long id) {
		return BoletaMapper.aRespuesta(boletas.findById(id).orElseThrow());
	}

	/** Catalogo confirmó: se guarda el detalle con los precios reales y se calculan los totales. */
	@Transactional
	public BoletaResponse confirmar(Long id, VentaCatalogo.Respuesta venta, Optional<Cupon> cupon) {
		Boleta b = boletas.findById(id).orElseThrow();
		if (b.estaPagada()) return BoletaMapper.aRespuesta(b); // ya confirmada en un intento anterior
		int subtotal = 0;
		for (VentaCatalogo.Linea l : venta.lineas()) {
			DetalleBoleta d = new DetalleBoleta(l.codigo(), l.nombre(), l.categoria(), l.cantidad(), l.precioUnitario());
			b.agregarDetalle(d);
			subtotal += d.getSubtotal();
		}
		final int base = subtotal;
		int descuento = cupon.map(c -> c.descuentoSobre(base)).orElse(0);
		b.setSubtotal(subtotal);
		b.setDescuento(descuento);
		b.setTotal(subtotal - descuento);
		b.setFecha(Instant.now(reloj));
		b.setEstado(Boleta.ESTADO_PAGADA);
		return BoletaMapper.aRespuesta(b);
	}

	/** Catalogo rechazó la compra (sin stock, producto inexistente): no se descontó nada, la pendiente sobra. */
	@Transactional
	public void eliminarPendiente(Long id) {
		boletas.findById(id).filter(b -> !b.estaPagada()).ifPresent(boletas::delete);
	}
}
