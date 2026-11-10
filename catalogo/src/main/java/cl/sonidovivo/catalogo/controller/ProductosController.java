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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.sonidovivo.catalogo.dto.ProductoResponse;
import cl.sonidovivo.catalogo.dto.ProductoRequest;
import cl.sonidovivo.common.exception.ErrorRespuesta;
import cl.sonidovivo.catalogo.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Catálogo. Lectura pública; escritura solo Administrador")
public class ProductosController {

	private final ProductoService servicio;

	public ProductosController(ProductoService servicio) {
		this.servicio = servicio;
	}

	@GetMapping
	@Operation(summary = "Listar productos", description = "Filtros opcionales combinables. Público.")
	public List<ProductoResponse> listar(
			@Parameter(description = "Id de categoría, ej. baterias") @RequestParam(required = false) String categoria,
			@Parameter(description = "Texto a buscar en nombre, marca, modelo o código") @RequestParam(required = false) String q,
			@Parameter(description = "true = solo productos en oferta") @RequestParam(required = false) Boolean oferta) {
		return servicio.listar(categoria, q, oferta);
	}

	@GetMapping("/criticos")
	@Operation(summary = "Productos en stock crítico", description = "stock <= stockCritico. Solo Administrador.",
			security = @SecurityRequirement(name = "bearerAuth"))
	public List<ProductoResponse> criticos() {
		return servicio.criticos();
	}

	@GetMapping("/{codigo}")
	@Operation(summary = "Detalle de un producto", description = "Público.")
	@ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public ProductoResponse obtener(@PathVariable String codigo) {
		return servicio.obtener(codigo);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Crear producto", description = "Solo Administrador.", security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "201", description = "Creado")
	@ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	@ApiResponse(responseCode = "409", description = "Código repetido", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public ProductoResponse crear(@Valid @RequestBody ProductoRequest datos) {
		return servicio.crear(datos);
	}

	@PutMapping("/{codigo}")
	@Operation(summary = "Actualizar producto", description = "El código no cambia. Solo Administrador.",
			security = @SecurityRequirement(name = "bearerAuth"))
	public ProductoResponse actualizar(@PathVariable String codigo, @Valid @RequestBody ProductoRequest datos) {
		return servicio.actualizar(codigo, datos);
	}

	@DeleteMapping("/{codigo}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Eliminar producto", description = "No se puede si tiene ventas registradas en catálogo. Solo Administrador.",
			security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "204", description = "Eliminado")
	@ApiResponse(responseCode = "409", description = "Tiene ventas registradas", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public void eliminar(@PathVariable String codigo) {
		servicio.eliminar(codigo);
	}
}
