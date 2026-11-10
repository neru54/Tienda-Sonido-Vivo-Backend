package cl.sonidovivo.catalogo.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

import cl.sonidovivo.common.exception.ApiErrors;

/**
 * @RestControllerAdvice aplica a todos los controladores de catalogo los @ExceptionHandler
 * heredados de common (mismo formato de error en los tres microservicios).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ApiErrors {
}
