package cl.sonidovivo.usuarios.dto;

import java.time.LocalDate;

import cl.sonidovivo.common.security.TipoUsuario;
import cl.sonidovivo.usuarios.model.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

/** Usuario tal como lo devuelve la API: NUNCA incluye la contraseña. */
@Schema(description = "Usuario (sin contraseña)")
public record UsuarioResponse(
		@Schema(example = "11111111-1") String run,
		@Schema(example = "María") String nombre,
		@Schema(example = "Pérez Soto") String apellidos,
		@Schema(example = "maria.perez@gmail.com") String correo,
		LocalDate fechaNacimiento,
		@Schema(example = "Administrador", allowableValues = { "Administrador", "Vendedor", "Cliente" }) TipoUsuario tipoUsuario,
		String region,
		String comuna,
		String direccion) {

	public static UsuarioResponse desde(Usuario u) {
		return new UsuarioResponse(u.getRun(), u.getNombre(), u.getApellidos(), u.getCorreo(), u.getFechaNacimiento(),
				u.getTipoUsuario(), u.getRegion(), u.getComuna(), u.getDireccion());
	}
}
