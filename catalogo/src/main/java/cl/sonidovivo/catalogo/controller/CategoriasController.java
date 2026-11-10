package cl.sonidovivo.catalogo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.sonidovivo.catalogo.dto.CategoriaResponse;
import cl.sonidovivo.catalogo.dto.CategoriaRequest;
import cl.sonidovivo.common.exception.ErrorRespuesta;
import cl.sonidovivo.catalogo.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorías", description = "Lectura pública; escritura solo Administrador")
public class CategoriasController {

	private final CategoriaService servicio;

	public CategoriasController(CategoriaService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@Operation(summary = "Listar categorías", description = "Incluye cuántos productos tiene cada una. Público.")
	public List<CategoriaResponse> listar() {
		return servicio.listar();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Detalle de una categoría", description = "Público.")
	@ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public CategoriaResponse obtener(@PathVariable String id) {
		return servicio.obtener(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Crear categoría", description = "El id se deriva del nombre. Solo Administrador.",
			security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "201", description = "Creada")
	@ApiResponse(responseCode = "409", description = "Nombre repetido", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public CategoriaResponse crear(@Valid @RequestBody CategoriaRequest datos) {
		return servicio.crear(datos);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Actualizar categoría", description = "Renombrar no cambia el id. Solo Administrador.",
			security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "409", description = "Nombre repetido", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public CategoriaResponse actualizar(@PathVariable String id, @Valid @RequestBody CategoriaRequest datos) {
		return servicio.actualizar(id, datos);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Eliminar categoría", description = "Solo si no tiene productos. Solo Administrador.",
			security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "204", description = "Eliminada")
	@ApiResponse(responseCode = "409", description = "Tiene productos asociados", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public void eliminar(@PathVariable String id) {
		servicio.eliminar(id);
	}
}
