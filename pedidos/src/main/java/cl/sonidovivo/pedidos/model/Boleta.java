package cl.sonidovivo.pedidos.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Tabla BOLETA (cabecera de la compra) del esquema sonido_pedidos.
 *
 *  - Número de boleta desde 1001: secuencia "boleta_seq" (en MySQL Hibernate la emula con una tabla).
 *  - usuario_run es solo un TEXTO, sin clave foránea: el usuario vive en otro esquema (sonido_usuarios).
 *    NULL si se compró sin cuenta. Si el usuario se elimina, la boleta conserva su RUN (dato histórico).
 *  - Los datos del comprador y de la entrega se copian aquí para que el comprobante no cambie después.
 *  - estado: "Pendiente" mientras catalogo no confirma el stock; "Pagada" cuando la compra terminó.
 *    Listados, historial y reportes muestran solo las pagadas.
 *  - idempotency_key + huella: si el navegador reintenta con la misma clave, no se crea otra boleta.
 */
@Entity
@Table(name = "boleta")
public class Boleta {

	public static final String ESTADO_PENDIENTE = "Pendiente";
	public static final String ESTADO_PAGADA = "Pagada";

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "boleta_seq")
	@SequenceGenerator(name = "boleta_seq", sequenceName = "boleta_seq", initialValue = 1001, allocationSize = 1)
	private Long id;

	@Column(nullable = false)
	private Instant fecha;

	@Column(nullable = false, length = 20)
	private String estado;

	@Column(name = "usuario_run", length = 12)
	private String usuarioRun;

	@Column(name = "nombre_cliente", nullable = false, length = 50)
	private String nombreCliente;

	@Column(name = "apellidos_cliente", nullable = false, length = 100)
	private String apellidosCliente;

	@Column(name = "correo_cliente", nullable = false, length = 100)
	private String correoCliente;

	@Column(nullable = false, length = 150)
	private String calle;

	@Column(length = 20)
	private String departamento;

	@Column(nullable = false, length = 100)
	private String region;

	@Column(nullable = false, length = 100)
	private String comuna;

	@Column(length = 300)
	private String indicaciones;

	@Column(length = 30)
	private String cupon;

	@Column(nullable = false)
	private Integer subtotal = 0;

	@Column(nullable = false)
	private Integer descuento = 0;

	@Column(nullable = false)
	private Integer total = 0;

	@Column(name = "idempotency_key", unique = true, length = 64)
	private String idempotencyKey;

	/** Resumen (SHA-256) del contenido de la compra: detecta la misma clave usada con otro carrito. */
	@Column(length = 64)
	private String huella;

	@OneToMany(mappedBy = "boleta", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("id ASC")
	private List<DetalleBoleta> detalles = new ArrayList<>();

	public void agregarDetalle(DetalleBoleta detalle) {
		detalle.setBoleta(this);
		detalles.add(detalle);
	}

	public boolean estaPagada() {
		return ESTADO_PAGADA.equals(estado);
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Instant getFecha() { return fecha; }
	public void setFecha(Instant fecha) { this.fecha = fecha; }
	public String getEstado() { return estado; }
	public void setEstado(String estado) { this.estado = estado; }
	public String getUsuarioRun() { return usuarioRun; }
	public void setUsuarioRun(String usuarioRun) { this.usuarioRun = usuarioRun; }
	public String getNombreCliente() { return nombreCliente; }
	public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }
	public String getApellidosCliente() { return apellidosCliente; }
	public void setApellidosCliente(String apellidosCliente) { this.apellidosCliente = apellidosCliente; }
	public String getCorreoCliente() { return correoCliente; }
	public void setCorreoCliente(String correoCliente) { this.correoCliente = correoCliente; }
	public String getCalle() { return calle; }
	public void setCalle(String calle) { this.calle = calle; }
	public String getDepartamento() { return departamento; }
	public void setDepartamento(String departamento) { this.departamento = departamento; }
	public String getRegion() { return region; }
	public void setRegion(String region) { this.region = region; }
	public String getComuna() { return comuna; }
	public void setComuna(String comuna) { this.comuna = comuna; }
	public String getIndicaciones() { return indicaciones; }
	public void setIndicaciones(String indicaciones) { this.indicaciones = indicaciones; }
	public String getCupon() { return cupon; }
	public void setCupon(String cupon) { this.cupon = cupon; }
	public Integer getSubtotal() { return subtotal; }
	public void setSubtotal(Integer subtotal) { this.subtotal = subtotal; }
	public Integer getDescuento() { return descuento; }
	public void setDescuento(Integer descuento) { this.descuento = descuento; }
	public Integer getTotal() { return total; }
	public void setTotal(Integer total) { this.total = total; }
	public String getIdempotencyKey() { return idempotencyKey; }
	public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
	public String getHuella() { return huella; }
	public void setHuella(String huella) { this.huella = huella; }
	public List<DetalleBoleta> getDetalles() { return detalles; }
	public void setDetalles(List<DetalleBoleta> detalles) { this.detalles = detalles; }
}
