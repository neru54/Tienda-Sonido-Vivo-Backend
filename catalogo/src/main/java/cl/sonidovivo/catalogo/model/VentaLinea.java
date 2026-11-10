package cl.sonidovivo.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Línea del comprobante de venta (tabla VENTA_LINEA). Guarda el precio, el nombre y la categoría
 * que tenía el producto EN ESE MOMENTO: así un reintento devuelve exactamente lo mismo aunque
 * después el administrador cambie el precio o renombre el producto.
 */
@Embeddable
public class VentaLinea {

	@Column(nullable = false, length = 20)
	private String codigo;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(nullable = false, length = 50)
	private String categoria;

	@Column(nullable = false)
	private Integer cantidad;

	@Column(name = "precio_unitario", nullable = false)
	private Integer precioUnitario;

	public VentaLinea() {
	}

	public VentaLinea(String codigo, String nombre, String categoria, int cantidad, int precioUnitario) {
		this.codigo = codigo;
		this.nombre = nombre;
		this.categoria = categoria;
		this.cantidad = cantidad;
		this.precioUnitario = precioUnitario;
	}

	public int subtotal() {
		return cantidad * precioUnitario;
	}

	public String getCodigo() { return codigo; }
	public String getNombre() { return nombre; }
	public String getCategoria() { return categoria; }
	public Integer getCantidad() { return cantidad; }
	public Integer getPrecioUnitario() { return precioUnitario; }
}
