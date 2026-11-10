package cl.sonidovivo.pedidos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Tabla DETALLE_BOLETA: una fila por producto comprado.
 *
 * Diferencia con el monolito: allí tenía una FK a PRODUCTO. Aquí el producto vive en otro esquema,
 * así que se guarda su CÓDIGO (referencia por ID) y además una copia del nombre y la categoría que
 * tenía al momento de la venta. Con eso los reportes se calculan sin consultar a catalogo, y la
 * boleta no cambia aunque después el producto se renombre o cambie de precio.
 */
@Entity
@Table(name = "detalle_boleta")
public class DetalleBoleta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "boleta_id", nullable = false)
	private Boleta boleta;

	@Column(name = "codigo_producto", nullable = false, length = 20)
	private String codigoProducto;

	@Column(name = "nombre_producto", nullable = false, length = 100)
	private String nombreProducto;

	@Column(name = "categoria_producto", nullable = false, length = 50)
	private String categoriaProducto;

	@Column(nullable = false)
	private Integer cantidad;

	@Column(name = "precio_unitario", nullable = false)
	private Integer precioUnitario;

	@Column(nullable = false)
	private Integer subtotal;

	public DetalleBoleta() {
	}

	public DetalleBoleta(String codigo, String nombre, String categoria, int cantidad, int precioUnitario) {
		this.codigoProducto = codigo;
		this.nombreProducto = nombre;
		this.categoriaProducto = categoria;
		this.cantidad = cantidad;
		this.precioUnitario = precioUnitario;
		this.subtotal = cantidad * precioUnitario;
	}

	public Long getId() { return id; }
	public Boleta getBoleta() { return boleta; }
	public void setBoleta(Boleta boleta) { this.boleta = boleta; }
	public String getCodigoProducto() { return codigoProducto; }
	public String getNombreProducto() { return nombreProducto; }
	public String getCategoriaProducto() { return categoriaProducto; }
	public Integer getCantidad() { return cantidad; }
	public Integer getPrecioUnitario() { return precioUnitario; }
	public Integer getSubtotal() { return subtotal; }
}
