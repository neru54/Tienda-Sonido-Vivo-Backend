package cl.sonidovivo.pedidos.mapper;

import cl.sonidovivo.pedidos.dto.BoletaResponse;
import cl.sonidovivo.pedidos.model.Boleta;
import cl.sonidovivo.pedidos.model.DetalleBoleta;

/**
 * Convierte la entidad Boleta en el DTO de respuesta. Separa lo que se guarda (idempotency_key, huella)
 * de lo que se publica: esos dos campos internos nunca salen en el JSON.
 * Métodos estáticos: no necesita ser un bean de Spring.
 */
public final class BoletaMapper {

	private BoletaMapper() {
	}

	public static BoletaResponse aRespuesta(Boleta b) {
		return new BoletaResponse(b.getId(), b.getFecha(), b.getEstado(),
				new BoletaResponse.Cliente(b.getUsuarioRun(), b.getNombreCliente(), b.getApellidosCliente(), b.getCorreoCliente()),
				new BoletaResponse.Direccion(b.getCalle(), b.getDepartamento(), b.getRegion(), b.getComuna(), b.getIndicaciones()),
				b.getDetalles().stream().map(BoletaMapper::aDetalle).toList(),
				b.getCupon(), b.getSubtotal(), b.getDescuento(), b.getTotal());
	}

	static BoletaResponse.Detalle aDetalle(DetalleBoleta d) {
		return new BoletaResponse.Detalle(d.getCodigoProducto(), d.getNombreProducto(), d.getCategoriaProducto(),
				d.getPrecioUnitario(), d.getCantidad(), d.getSubtotal());
	}
}
