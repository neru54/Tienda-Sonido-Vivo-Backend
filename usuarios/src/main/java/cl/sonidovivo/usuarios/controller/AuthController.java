package cl.sonidovivo.usuarios.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cl.sonidovivo.common.exception.ErrorRespuesta;
import cl.sonidovivo.common.security.UsuarioToken;
import cl.sonidovivo.usuarios.dto.AuthResponse;
import cl.sonidovivo.usuarios.dto.LoginRequest;
import cl.sonidovivo.usuarios.dto.RegistroRequest;
import cl.sonidovivo.usuarios.dto.UsuarioResponse;
import cl.sonidovivo.usuarios.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Login, registro y datos de la sesión. El controlador solo recibe HTTP y delega en AuthService. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Login, registro de clientes y datos de la sesión")
public class AuthController {

	private final AuthService servicio;

	public AuthController(AuthService servicio) {
		this.servicio = servicio;
	}

	@PostMapping("/login")
	@Operation(summary = "Iniciar sesión", description = "Devuelve un JWT (2 horas por defecto) y los datos del usuario. Público.")
	@ApiResponse(responseCode = "200", description = "Credenciales correctas")
	@ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public AuthResponse login(@Valid @RequestBody LoginRequest datos) {
		return servicio.login(datos);
	}

	@PostMapping("/registro")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Registrarse como cliente", description = "Crea una cuenta con rol Cliente e inicia la sesión. Público.")
	@ApiResponse(responseCode = "201", description = "Cuenta creada")
	@ApiResponse(responseCode = "400", description = "Datos inválidos (RUN, correo, contraseña...)", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	@ApiResponse(responseCode = "409", description = "RUN o correo ya registrados", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public AuthResponse registrar(@Valid @RequestBody RegistroRequest datos) {
		return servicio.registrar(datos);
	}

	@GetMapping("/me")
	@Operation(summary = "Usuario de la sesión", description = "Datos del dueño del token. Requiere estar autenticado (cualquier rol).",
			security = @SecurityRequirement(name = "bearerAuth"))
	@ApiResponse(responseCode = "401", description = "Sin token o token vencido", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
	public UsuarioResponse yo(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioToken usuario) {
		return servicio.yo(usuario.run());
	}
}
