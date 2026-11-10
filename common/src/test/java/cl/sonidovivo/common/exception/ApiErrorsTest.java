package cl.sonidovivo.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/** Cada excepción se traduce al código HTTP correcto con el formato ErrorRespuesta (lo que lee el frontend). */
class ApiErrorsTest {

	/** Igual que el GlobalExceptionHandler de cada microservicio. */
	@RestControllerAdvice
	static class Manejador extends ApiErrors {
	}

	record Datos(@NotBlank(message = "El nombre es obligatorio.") String nombre) {
	}

	@RestController
	static class Prueba {
		@GetMapping("/error/{tipo}")
		String lanzar(@PathVariable String tipo) {
			switch (tipo) {
				case "404" -> throw new RecursoNoEncontradoException("No se encontró el producto solicitado.");
				case "409" -> throw new ConflictoException("Ya existe un producto con ese código.");
				case "400" -> throw new ReglaNegocioException("El cupón \"X\" no es válido.");
				case "503" -> throw new ServicioNoDisponibleException("El catálogo no responde.");
				default -> throw new IllegalStateException("inesperado");
			}
		}

		@PostMapping("/validar")
		String validar(@Valid @RequestBody Datos d) {
			return d.nombre();
		}
	}

	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new Prueba()).setControllerAdvice(new Manejador()).build();

	@Test
	void excepcionesDeNegocioConSuCodigo() throws Exception {
		mvc.perform(get("/error/404")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.estado").value(404))
				.andExpect(jsonPath("$.mensaje").value("No se encontró el producto solicitado."))
				.andExpect(jsonPath("$.ruta").value("/error/404"));
		mvc.perform(get("/error/409")).andExpect(status().isConflict());
		mvc.perform(get("/error/400")).andExpect(status().isBadRequest());
		mvc.perform(get("/error/503")).andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.error").value("Service Unavailable"));
	}

	@Test
	void errorInesperadoNoMuestraDetallesInternos() throws Exception {
		mvc.perform(get("/error/otro")).andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.mensaje").value("Ocurrió un error inesperado. Inténtalo nuevamente."));
	}

	@Test
	void validacionYJsonMalFormado() throws Exception {
		mvc.perform(post("/validar").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("El nombre es obligatorio."))
				.andExpect(jsonPath("$.errores.nombre").value("El nombre es obligatorio."));
		mvc.perform(post("/validar").contentType(MediaType.APPLICATION_JSON).content("{malo"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.mensaje").value("La solicitud no tiene el formato esperado."));
	}
}
