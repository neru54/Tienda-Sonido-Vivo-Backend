package cl.sonidovivo.common.exception;

import org.springframework.http.HttpStatus;

/** Otro microservicio no respondió (por ejemplo, catalogo durante una compra): 503. */
public class ServicioNoDisponibleException extends ApiException {

	public ServicioNoDisponibleException(String mensaje) {
		super(HttpStatus.SERVICE_UNAVAILABLE, mensaje);
	}
}
