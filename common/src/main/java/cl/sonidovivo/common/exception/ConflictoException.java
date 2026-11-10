package cl.sonidovivo.common.exception;

import org.springframework.http.HttpStatus;

/** La operación choca con el estado actual de los datos (duplicados, stock, dependencias): 409. */
public class ConflictoException extends ApiException {

	public ConflictoException(String mensaje) {
		super(HttpStatus.CONFLICT, mensaje);
	}
}
