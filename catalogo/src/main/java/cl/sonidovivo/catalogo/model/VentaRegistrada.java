package cl.sonidovivo.catalogo.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

/**
 * Tabla VENTA_REGISTRADA: comprobante PROPIO de catalogo por cada descuento de stock que pidió pedidos.
 *
 * ¿Para qué, si la boleta ya está en pedidos? Porque catalogo no puede leer el esquema de pedidos:
 *  1. Idempotencia: la clave es la "referencia" que envía pedidos (el número de boleta). Si pedidos
 *     reintenta la misma venta (se cortó la red), catalogo devuelve este comprobante SIN volver a
 *     descontar stock.
 *  2. Regla "no eliminar un producto con ventas": se revisa aquí mismo, en sus líneas.
 */
@Entity
@Table(name = "venta_registrada")
public class VentaRegistrada {

	@Id
	@Column(length = 64)
	private String referencia;

	@Column(nullable = false)
	private Instant fecha;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "venta_linea", joinColumns = @JoinColumn(name = "referencia"))
	@OrderColumn(name = "posicion")
	private List<VentaLinea> lineas = new ArrayList<>();

	public VentaRegistrada() {
	}

	public VentaRegistrada(String referencia, Instant fecha) {
		this.referencia = referencia;
		this.fecha = fecha;
	}

	public String getReferencia() { return referencia; }
	public void setReferencia(String referencia) { this.referencia = referencia; }
	public Instant getFecha() { return fecha; }
	public void setFecha(Instant fecha) { this.fecha = fecha; }
	public List<VentaLinea> getLineas() { return lineas; }
	public void setLineas(List<VentaLinea> lineas) { this.lineas = lineas; }
}
