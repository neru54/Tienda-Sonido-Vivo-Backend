package cl.sonidovivo.common.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Manejadores de error COMPARTIDOS. Esta clase no lleva @RestControllerAdvice: cada microservicio
 * declara su propio exception/GlobalExceptionHandler que la extiende y se anota con
 * @RestControllerAdvice (así lo pide la estructura de la referencia y evita registrar dos advices).
 *
 * Resultado: los tres servicios responden los errores con el mismo {@link ErrorRespuesta}.
 */
public abstract class ApiErrors {

	private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);

	/** Todas las excepciones de negocio (404, 409, 400, 503): el código viene en la propia excepción. */
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorRespuesta> negocio(ApiException e, HttpServletRequest req) {
		return responder(e.getEstado(), e.getMessage(), req);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorRespuesta> rutaInexistente(NoResourceFoundException e, HttpServletRequest req) {
		return responder(HttpStatus.NOT_FOUND, "La ruta solicitada no existe.", req);
	}

	/** Respaldo de las restricciones UNIQUE de la base: no se publican detalles SQL. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorRespuesta> integridad(DataIntegrityViolationException e, HttpServletRequest req) {
		log.warn("Violación de integridad en {}: {}", req.getRequestURI(), e.getMostSpecificCause().getMessage());
		return responder(HttpStatus.CONFLICT, "La operación entra en conflicto con datos existentes.", req);
	}

	/** @Valid falló: 400 con el primer mensaje y el mapa campo -> error. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException e, HttpServletRequest req) {
		Map<String, String> errores = new LinkedHashMap<>();
		e.getBindingResult().getFieldErrors().forEach(f -> errores.putIfAbsent(f.getField(), f.getDefaultMessage()));
		String mensaje = errores.isEmpty() ? "Los datos enviados no son válidos." : errores.values().iterator().next();
		HttpStatus estado = HttpStatus.BAD_REQUEST;
		return ResponseEntity.status(estado)
				.body(new ErrorRespuesta(Instant.now(), estado.value(), estado.getReasonPhrase(), mensaje, req.getRequestURI(), errores));
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class, MissingRequestHeaderException.class, IllegalArgumentException.class })
	public ResponseEntity<ErrorRespuesta> malFormado(Exception e, HttpServletRequest req) {
		return responder(HttpStatus.BAD_REQUEST, "La solicitud no tiene el formato esperado.", req);
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorRespuesta> credenciales(BadCredentialsException e, HttpServletRequest req) {
		return responder(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.", req);
	}

	/** Lo lanzan los servicios cuando el rol alcanza pero el recurso es de otra persona. */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorRespuesta> accesoDenegado(AccessDeniedException e, HttpServletRequest req) {
		return responder(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta acción.", req);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorRespuesta> metodo(HttpRequestMethodNotSupportedException e, HttpServletRequest req) {
		return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no permitido en esta ruta.", req);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorRespuesta> inesperado(Exception e, HttpServletRequest req) {
		log.error("Error inesperado en {}", req.getRequestURI(), e);
		return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Inténtalo nuevamente.", req);
	}

	protected ResponseEntity<ErrorRespuesta> responder(HttpStatus estado, String mensaje, HttpServletRequest req) {
		return ResponseEntity.status(estado)
				.body(ErrorRespuesta.de(estado.value(), estado.getReasonPhrase(), mensaje, req.getRequestURI()));
	}
}
