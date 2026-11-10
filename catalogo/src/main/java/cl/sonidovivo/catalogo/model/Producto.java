package cl.sonidovivo.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Tabla PRODUCTO. Cada producto pertenece a una CATEGORIA (FK categoria_id): esta relación JPA sí
 * existe porque ambas tablas están en el MISMO esquema (sonido_catalogo). Precios en CLP (enteros).
 */
@Entity
@Table(name = "producto")
public class Producto {

	public static final int STOCK_CRITICO_POR_DEFECTO = 3;

	@Id
	@Column(length = 20)
	private String codigo;

	@Column(nullable = false, length = 100)
	private String nombre;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "categoria_id", nullable = false)
	private Categoria categoria;

	@Column(length = 60)
	private String marca;

	@Column(length = 60)
	private String modelo;

	@Column(length = 500)
	private String descripcion;

	@Column(length = 500)
	private String imagen;

	@Column(nullable = false)
	private Integer precio;

	@Column(nullable = false)
	private Integer stock;

	@Column(name = "stock_critico", nullable = false)
	private Integer stockCritico = STOCK_CRITICO_POR_DEFECTO;

	@Column(nullable = false)
	private boolean oferta;

	@Column(name = "precio_oferta")
	private Integer precioOferta;

	/** Precio que realmente se cobra: el de oferta si el producto está en oferta. */
	public int precioVigente() {
		return oferta && precioOferta != null ? precioOferta : precio;
	}

	/** Stock crítico: stock menor o igual al umbral del producto. */
	public boolean esStockCritico() {
		return stock <= stockCritico;
	}

	public String getCodigo() { return codigo; }
	public void setCodigo(String codigo) { this.codigo = codigo; }
	public String getNombre() { return nombre; }
	public void setNombre(String nombre) { this.nombre = nombre; }
	public Categoria getCategoria() { return categoria; }
	public void setCategoria(Categoria categoria) { this.categoria = categoria; }
	public String getMarca() { return marca; }
	public void setMarca(String marca) { this.marca = marca; }
	public String getModelo() { return modelo; }
	public void setModelo(String modelo) { this.modelo = modelo; }
	public String getDescripcion() { return descripcion; }
	public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
	public String getImagen() { return imagen; }
	public void setImagen(String imagen) { this.imagen = imagen; }
	public Integer getPrecio() { return precio; }
	public void setPrecio(Integer precio) { this.precio = precio; }
	public Integer getStock() { return stock; }
	public void setStock(Integer stock) { this.stock = stock; }
	public Integer getStockCritico() { return stockCritico; }
	public void setStockCritico(Integer stockCritico) { this.stockCritico = stockCritico; }
	public boolean isOferta() { return oferta; }
	public void setOferta(boolean oferta) { this.oferta = oferta; }
	public Integer getPrecioOferta() { return precioOferta; }
	public void setPrecioOferta(Integer precioOferta) { this.precioOferta = precioOferta; }
}
