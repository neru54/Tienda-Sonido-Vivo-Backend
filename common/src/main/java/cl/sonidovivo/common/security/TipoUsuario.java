package cl.sonidovivo.common.security;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Roles del sistema (enunciado de la Evaluación 3). Está en common porque viaja dentro del JWT
 * y los tres servicios deben entenderlo igual.
 * En la base se guarda el nombre de la constante (ADMINISTRADOR...); en el JSON viaja la
 * etiqueta ("Administrador"...), que es lo que el frontend ya usaba.
 * La autoridad de Spring Security es "ROLE_" + name(), por ejemplo ROLE_ADMINISTRADOR.
 */
public enum TipoUsuario {
	ADMINISTRADOR("Administrador"),
	VENDEDOR("Vendedor"),
	CLIENTE("Cliente");

	private final String etiqueta;

	TipoUsuario(String etiqueta) {
		this.etiqueta = etiqueta;
	}

	@JsonValue
	public String getEtiqueta() {
		return etiqueta;
	}

	public String autoridad() {
		return "ROLE_" + name();
	}

	/** Acepta "Administrador", "ADMINISTRADOR" o "administrador". */
	@JsonCreator
	public static TipoUsuario desde(String texto) {
		if (texto == null || texto.isBlank()) return null;
		for (TipoUsuario t : values()) {
			if (t.etiqueta.equalsIgnoreCase(texto.trim()) || t.name().equalsIgnoreCase(texto.trim())) return t;
		}
		throw new IllegalArgumentException("Tipo de usuario desconocido: " + texto);
	}
}
