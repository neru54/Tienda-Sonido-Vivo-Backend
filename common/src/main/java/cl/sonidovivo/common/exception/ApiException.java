package cl.sonidovivo.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción de negocio con su código HTTP. Las subclases (404, 409, 400, 503) dejan el código
 * explícito en el nombre, y ApiErrors las traduce todas con un solo método.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus estado;

	public ApiException(HttpStatus estado, String mensaje) {
		super(mensaje);
		this.estado = estado;
	}

	public HttpStatus getEstado() {
		return estado;
	}
}
