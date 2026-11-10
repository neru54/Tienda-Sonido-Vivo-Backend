package cl.sonidovivo.pedidos.client;

import java.time.Instant;
import java.util.List;

/**
 * Contrato de la llamada interna pedidos -> catalogo (POST /internal/ventas).
 * Son copias de los DTO de catalogo: cada microservicio declara sus propios tipos y solo
 * comparten el formato JSON, así pueden desplegarse por separado.
 */
public final class VentaCatalogo {

	private VentaCatalogo() {
	}

	public record Peticion(String referencia, List<Item> items) {
	}

	public record Item(String codigo, int cantidad) {
	}

	public record Respuesta(String referencia, Instant fecha, List<Linea> lineas, int subtotal) {
	}

	public record Linea(String codigo, String nombre, String categoria, int cantidad, int precioUnitario, int subtotal) {
	}

	public record Inventario(long unidadesEnStock, long productos) {
	}
}
