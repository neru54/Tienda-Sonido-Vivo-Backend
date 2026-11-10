package cl.sonidovivo.usuarios.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.sonidovivo.common.exception.ErrorRespuesta;
import cl.sonidovivo.common.security.UsuarioToken;
import cl.sonidovivo.usuarios.dto.PerfilRequest;
import cl.sonidovivo.usuarios.dto.UsuarioRequest;
import cl.sonidovivo.usuarios.dto.UsuarioResponse;
import cl.sonidovivo.usuarios.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * CRUD de usuarios (solo Administrador) y perfil propio (/me).
 * El historial de compras (/usuarios/me/boletas y /usuarios/{run}/boletas) lo atiende el
 * microservicio pedidos: el Gateway enruta esas dos rutas hacia allá.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "CRUD solo Administrador; /me para la propia cuenta")
@SecurityRequirement(name = "bearerAuth")
public class UsuariosController {

	private final UsuarioService servicio;

	public UsuariosController(UsuarioService servicio) {
		this.servicio = servicio;
	}

	/* ---------- Propia cuenta (Administrador o Cliente) ---------- */

	@GetMapping("/me")
	@Operation(summary = "Mi perfil", description = "Administrador o Cliente.")
	public UsuarioResponse miPerfil(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioToken yo) {
		return servicio.obtener(yo.run());
	}

	@PutMapping("/me")
	@Operation(summary = "Actualizar mi perfil", description = "No permite cambiar RUN, correo ni tipo de usuario.")
	public UsuarioResponse actualizarPerfil(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioToken yo,
			@Valid @RequestBody PerfilRequest datos) {
		return servicio.actualizarPerfil(yo.run(), datos);
	}

	/* ---------- Administración (solo Administrador) ---------- */

	@GetMapping
	@Operation(summary = "Listar usuarios", description = "Solo Administrador.")
	public List<UsuarioResponse> listar() {
		return servicio.listar();
	}

	@GetMapping("/{run}")
	@Operation(summary = "Detalle de un usuario", description = "Solo Administrador.")
	@ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public UsuarioResponse obtener(@PathVariable String run) {
		return servicio.obtener(run);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Crear usuario", description = "Cualquier rol. Solo Administrador.")
	@ApiResponse(responseCode = "201", description = "Creado")
	@ApiResponse(responseCode = "409", description = "RUN o correo repetido", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest datos) {
		return servicio.crear(datos);
	}

	@PutMapping("/{run}")
	@Operation(summary = "Actualizar usuario", description = "Contraseña vacía = se conserva. Solo Administrador.")
	@ApiResponse(responseCode = "409", description = "Correo usado por otro usuario", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public UsuarioResponse actualizar(@PathVariable String run, @Valid @RequestBody UsuarioRequest datos) {
		return servicio.actualizar(run, datos);
	}

	@DeleteMapping("/{run}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Eliminar usuario", description = "No permite eliminarse a sí mismo; sus boletas se conservan con su RUN. Solo Administrador.")
	@ApiResponse(responseCode = "204", description = "Eliminado")
	@ApiResponse(responseCode = "409", description = "Intento de eliminar la propia cuenta", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public void eliminar(@PathVariable String run, @Parameter(hidden = true) @AuthenticationPrincipal UsuarioToken yo) {
		servicio.eliminar(run, yo.run());
	}
}
