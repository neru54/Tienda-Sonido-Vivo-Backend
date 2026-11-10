package cl.sonidovivo.usuarios.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.sonidovivo.common.exception.ConflictoException;
import cl.sonidovivo.common.exception.RecursoNoEncontradoException;
import cl.sonidovivo.common.exception.ReglaNegocioException;
import cl.sonidovivo.common.security.TipoUsuario;
import cl.sonidovivo.common.util.Patrones;
import cl.sonidovivo.common.util.Textos;
import cl.sonidovivo.usuarios.dto.PerfilRequest;
import cl.sonidovivo.usuarios.dto.UsuarioRequest;
import cl.sonidovivo.usuarios.dto.UsuarioResponse;
import cl.sonidovivo.usuarios.model.Usuario;
import cl.sonidovivo.usuarios.repository.UsuarioRepository;

/**
 * Reglas de usuarios (las mismas del monolito):
 *  - RUN válido (dígito verificador) y único; correo único (sin distinguir mayúsculas).
 *  - Contraseña obligatoria al crear (4 a 10 caracteres); al editar, vacía = se conserva. Se guarda con BCrypt.
 *  - Nadie puede eliminarse a sí mismo.
 *  - En el perfil propio no se puede cambiar RUN, correo ni tipo de usuario.
 *
 * Cambio respecto del monolito: al eliminar un usuario sus boletas NO se tocan. Están en otro
 * microservicio (pedidos) y conservan el RUN como referencia histórica del comprador; el RUN es un
 * identificador único de la persona, por lo que sigue apuntando a quien compró.
 */
@Service
@Transactional(readOnly = true)
public class UsuarioService {

	private final UsuarioRepository usuarios;
	private final PasswordEncoder encoder;

	public UsuarioService(UsuarioRepository usuarios, PasswordEncoder encoder) {
		this.usuarios = usuarios;
		this.encoder = encoder;
	}

	public List<UsuarioResponse> listar() {
		return usuarios.findAllByOrderByRunAsc().stream().map(UsuarioResponse::desde).toList();
	}

	public UsuarioResponse obtener(String run) {
		return UsuarioResponse.desde(buscar(run));
	}

	/** Alta genérica: la usan el panel (cualquier rol) y el registro de la tienda (siempre Cliente). */
	@Transactional
	public Usuario crear(String runTexto, String nombre, String apellidos, String correoTexto, String password,
			TipoUsuario tipo, String region, String comuna, String direccion, LocalDate fechaNacimiento) {
		String run = Textos.normalizarRun(runTexto);
		if (!Textos.runValido(run)) throw new ReglaNegocioException(Patrones.MSJ_RUN);
		String correo = correoTexto.trim();
		if (usuarios.existsByCorreoIgnoreCase(correo)) throw new ConflictoException("Ya existe un usuario con ese correo.");
		if (usuarios.existsById(run)) throw new ConflictoException("Ya existe un usuario con ese RUN.");
		validarPassword(password, true);

		Usuario u = new Usuario();
		u.setRun(run);
		u.setNombre(nombre.trim());
		u.setApellidos(apellidos.trim());
		u.setCorreo(correo);
		u.setPassword(encoder.encode(password));
		u.setTipoUsuario(tipo);
		u.setRegion(region);
		u.setComuna(comuna);
		u.setDireccion(direccion.trim());
		u.setFechaNacimiento(fechaNacimiento);
		return usuarios.save(u);
	}

	@Transactional
	public UsuarioResponse crear(UsuarioRequest d) {
		return UsuarioResponse.desde(crear(d.run(), d.nombre(), d.apellidos(), d.correo(), d.password(), d.tipoUsuario(),
				d.region(), d.comuna(), d.direccion(), d.fechaNacimiento()));
	}

	/** Edición desde el panel. El RUN de la URL manda (el del cuerpo se ignora). */
	@Transactional
	public UsuarioResponse actualizar(String run, UsuarioRequest d) {
		Usuario u = buscar(run);
		String correo = d.correo().trim();
		if (usuarios.existsByCorreoIgnoreCaseAndRunNot(correo, u.getRun())) {
			throw new ConflictoException("Ya existe un usuario con ese correo.");
		}
		u.setNombre(d.nombre().trim());
		u.setApellidos(d.apellidos().trim());
		u.setCorreo(correo);
		u.setTipoUsuario(d.tipoUsuario());
		u.setRegion(d.region());
		u.setComuna(d.comuna());
		u.setDireccion(d.direccion().trim());
		u.setFechaNacimiento(d.fechaNacimiento());
		cambiarPasswordSiViene(u, d.password());
		return UsuarioResponse.desde(u);
	}

	@Transactional
	public UsuarioResponse actualizarPerfil(String run, PerfilRequest d) {
		Usuario u = buscar(run);
		u.setNombre(d.nombre().trim());
		u.setApellidos(d.apellidos().trim());
		u.setRegion(d.region());
		u.setComuna(d.comuna());
		u.setDireccion(d.direccion().trim());
		u.setFechaNacimiento(d.fechaNacimiento());
		cambiarPasswordSiViene(u, d.password());
		return UsuarioResponse.desde(u);
	}

	@Transactional
	public void eliminar(String run, String runSesion) {
		Usuario u = buscar(run);
		if (u.getRun().equals(runSesion)) {
			throw new ConflictoException("No puedes eliminar tu propio usuario mientras tienes la sesión iniciada.");
		}
		usuarios.delete(u);
	}

	Usuario buscar(String run) {
		return usuarios.findById(Textos.normalizarRun(run))
				.orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario."));
	}

	private void cambiarPasswordSiViene(Usuario u, String password) {
		if (password != null && !password.isBlank()) {
			validarPassword(password, false);
			u.setPassword(encoder.encode(password));
		}
	}

	private static void validarPassword(String password, boolean obligatoria) {
		if (password == null || password.isBlank()) {
			if (obligatoria) throw new ReglaNegocioException("La contraseña es obligatoria.");
			return;
		}
		if (password.length() < 4 || password.length() > 10) {
			throw new ReglaNegocioException(Patrones.MSJ_PASSWORD);
		}
	}
}
