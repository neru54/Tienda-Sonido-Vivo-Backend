package cl.sonidovivo.pedidos.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Cupones de descuento vigentes. Viven en pedidos porque el descuento
 * se aplica sobre el subtotal de la boleta, después de que catalogo informa los precios reales.
 */
public enum Cupon {
	SONIDOVIVO10(0.10),
	BIENVENIDO5(0.05);

	private final double porcentaje;

	Cupon(double porcentaje) {
		this.porcentaje = porcentaje;
	}

	public double getPorcentaje() {
		return porcentaje;
	}

	/** Monto en CLP (entero, redondeado) que descuenta sobre un subtotal. */
	public int descuentoSobre(int subtotal) {
		return (int) Math.round(subtotal * porcentaje);
	}

	/** Busca un cupón sin distinguir mayúsculas ni espacios. Vacío si no existe. */
	public static Optional<Cupon> buscar(String texto) {
		if (texto == null || texto.isBlank()) return Optional.empty();
		String codigo = texto.trim().toUpperCase(Locale.ROOT);
		return Arrays.stream(values()).filter(c -> c.name().equals(codigo)).findFirst();
	}
}
