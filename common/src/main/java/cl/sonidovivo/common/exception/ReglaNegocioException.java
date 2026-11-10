package cl.sonidovivo.common.exception;

import org.springframework.http.HttpStatus;

/** Los datos son válidos en forma pero violan una regla de negocio (cupón inválido, RUN incorrecto...): 400. */
public class ReglaNegocioException extends ApiException {

	public ReglaNegocioException(String mensaje) {
		super(HttpStatus.BAD_REQUEST, mensaje);
	}
}
