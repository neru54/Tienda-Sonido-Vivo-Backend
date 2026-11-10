package cl.sonidovivo.usuarios.model;

import java.time.LocalDate;

import cl.sonidovivo.common.security.TipoUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tabla USUARIO del esquema sonido_usuarios. Clave primaria: el RUN (sin puntos y con guion).
 * La contraseña se guarda con BCrypt (nunca en texto plano).
 *
 * No tiene relación JPA con BOLETA: las boletas viven en otro esquema (sonido_pedidos) y
 * guardan solo el RUN como referencia. Así cada microservicio es dueño de sus tablas.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

	@Id
	@Column(length = 12)
	private String run;

	@Column(nullable = false, length = 50)
	private String nombre;

	@Column(nullable = false, length = 100)
	private String apellidos;

	@Column(nullable = false, unique = true, length = 100)
	private String correo;

	@Column(nullable = false, length = 100)
	private String password;

	@Column(name = "fecha_nacimiento")
	private LocalDate fechaNacimiento;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_usuario", nullable = false, length = 20)
	private TipoUsuario tipoUsuario;

	@Column(length = 100)
	private String region;

	@Column(length = 100)
	private String comuna;

	@Column(length = 300)
	private String direccion;

	public String getRun() { return run; }
	public void setRun(String run) { this.run = run; }
	public String getNombre() { return nombre; }
	public void setNombre(String nombre) { this.nombre = nombre; }
	public String getApellidos() { return apellidos; }
	public void setApellidos(String apellidos) { this.apellidos = apellidos; }
	public String getCorreo() { return correo; }
	public void setCorreo(String correo) { this.correo = correo; }
	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }
	public LocalDate getFechaNacimiento() { return fechaNacimiento; }
	public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
	public TipoUsuario getTipoUsuario() { return tipoUsuario; }
	public void setTipoUsuario(TipoUsuario tipoUsuario) { this.tipoUsuario = tipoUsuario; }
	public String getRegion() { return region; }
	public void setRegion(String region) { this.region = region; }
	public String getComuna() { return comuna; }
	public void setComuna(String comuna) { this.comuna = comuna; }
	public String getDireccion() { return direccion; }
	public void setDireccion(String direccion) { this.direccion = direccion; }
}
