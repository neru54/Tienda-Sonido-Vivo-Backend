package cl.sonidovivo.usuarios.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.sonidovivo.common.security.JwtUtil;
import cl.sonidovivo.common.security.JwtUtil.TokenEmitido;
import cl.sonidovivo.common.security.TipoUsuario;
import cl.sonidovivo.usuarios.dto.AuthResponse;
import cl.sonidovivo.usuarios.dto.LoginRequest;
import cl.sonidovivo.usuarios.dto.RegistroRequest;
import cl.sonidovivo.usuarios.dto.UsuarioResponse;
import cl.sonidovivo.usuarios.model.Usuario;
import cl.sonidovivo.usuarios.repository.UsuarioRepository;

/**
 * Inicio de sesión y registro: verifican credenciales y entregan el JWT.
 * Este es el único lugar de todo el sistema donde se FIRMAN tokens.
 */
@Service
public class AuthService {

	private final UsuarioRepository usuarios;
	private final UsuarioService usuarioService;
	private final PasswordEncoder encoder;
	private final JwtUtil jwt;

	public AuthService(UsuarioRepository usuarios, UsuarioService usuarioService, PasswordEncoder encoder, JwtUtil jwt) {
		this.usuarios = usuarios;
		this.usuarioService = usuarioService;
		this.encoder = encoder;
		this.jwt = jwt;
	}

	/** Mismo mensaje si el correo no existe o la contraseña es incorrecta (no revela qué cuentas existen). */
	@Transactional(readOnly = true)
	public AuthResponse login(LoginRequest datos) {
		Usuario u = usuarios.findByCorreoIgnoreCase(datos.correo().trim())
				.filter(x -> encoder.matches(datos.password(), x.getPassword()))
				.orElseThrow(() -> new BadCredentialsException("Correo o contraseña incorrectos."));
		return respuesta(u);
	}

	/** Registro desde la tienda: siempre crea un Cliente (nadie puede auto-registrarse como administrador). */
	@Transactional
	public AuthResponse registrar(RegistroRequest d) {
		Usuario u = usuarioService.crear(d.run(), d.nombre(), d.apellidos(), d.correo(), d.password(), TipoUsuario.CLIENTE,
				d.region(), d.comuna(), d.direccion(), d.fechaNacimiento());
		return respuesta(u);
	}

	@Transactional(readOnly = true)
	public UsuarioResponse yo(String run) {
		return usuarioService.obtener(run);
	}

	private AuthResponse respuesta(Usuario u) {
		TokenEmitido t = jwt.generar(u.getRun(), u.getTipoUsuario(), u.getNombre(), u.getCorreo());
		return new AuthResponse(t.token(), "Bearer", t.expiraEn(), UsuarioResponse.desde(u));
	}
}
