package cl.sonidovivo.catalogo.dto;

import java.time.Instant;
import java.util.List;

import cl.sonidovivo.catalogo.model.VentaLinea;
import cl.sonidovivo.catalogo.model.VentaRegistrada;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Comprobante que catalogo devuelve a pedidos: precio REAL cobrado por cada producto, y su nombre y
 * categoría en ese momento (pedidos los guarda en el detalle para sus reportes sin volver a preguntar).
 */
@Schema(description = "Comprobante de la venta registrada en catálogo (uso interno)")
public record VentaResponse(String referencia, Instant fecha, List<Linea> lineas, int subtotal) {

	@Schema(name = "LineaVenta")
	public record Linea(String codigo, String nombre, String categoria, int cantidad, int precioUnitario, int subtotal) {

		static Linea desde(VentaLinea l) {
			return new Linea(l.getCodigo(), l.getNombre(), l.getCategoria(), l.getCantidad(), l.getPrecioUnitario(), l.subtotal());
		}
	}

	public static VentaResponse desde(VentaRegistrada v) {
		List<Linea> lineas = v.getLineas().stream().map(Linea::desde).toList();
		return new VentaResponse(v.getReferencia(), v.getFecha(), lineas, lineas.stream().mapToInt(Linea::subtotal).sum());
	}
}
