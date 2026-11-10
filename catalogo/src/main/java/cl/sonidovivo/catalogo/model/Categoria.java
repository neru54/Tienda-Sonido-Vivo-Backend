package cl.sonidovivo.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabla CATEGORIA. El id es el "slug" del nombre (guitarras-acusticas), que usan las URLs del frontend. */
@Entity
@Table(name = "categoria")
public class Categoria {

	@Id
	@Column(length = 60)
	private String id;

	@Column(nullable = false, unique = true, length = 50)
	private String nombre;

	@Column(length = 200)
	private String descripcion;

	@Column(length = 7)
	private String color;

	public Categoria() {
	}

	public Categoria(String id, String nombre, String descripcion, String color) {
		this.id = id;
		this.nombre = nombre;
		this.descripcion = descripcion;
		this.color = color;
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	public String getNombre() { return nombre; }
	public void setNombre(String nombre) { this.nombre = nombre; }
	public String getDescripcion() { return descripcion; }
	public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
	public String getColor() { return color; }
	public void setColor(String color) { this.color = color; }
}
