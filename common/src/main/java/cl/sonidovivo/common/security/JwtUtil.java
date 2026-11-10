package cl.sonidovivo.common.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Emite y valida los JWT firmados con HMAC-SHA256 (HS256).
 *
 * Decisión de arquitectura: un SECRETO COMPARTIDO (variable JWT_SECRET, la misma en los tres servicios).
 *  - usuarios usa {@link #generar}: es el único que conoce contraseñas y emite tokens.
 *  - catalogo y pedidos solo usan {@link #validar}: comprueban firma y vencimiento sin llamar a usuarios.
 * Con HS256 quien valida también podría firmar, por eso el secreto solo vive en variables de entorno
 * de servidores propios (nunca en el frontend ni en git).
 *
 * Contenido del token: sub = RUN, rol, nombre, correo, iat y exp. Nunca la contraseña:
 * un JWT se puede decodificar, solo la firma es secreta.
 */
public class JwtUtil {

	static final String CLAIM_ROL = "rol";
	static final String CLAIM_NOMBRE = "nombre";
	static final String CLAIM_CORREO = "correo";

	private final SecretKey clave;
	private final Duration duracion;

	/**
	 * @param secretoBase64 secreto en Base64; debe tener al menos 32 bytes (256 bits) o jjwt lo rechaza
	 * @param minutos       vigencia de los tokens emitidos
	 */
	public JwtUtil(String secretoBase64, long minutos) {
		this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
		this.duracion = Duration.ofMinutes(minutos);
	}

	public TokenEmitido generar(String run, TipoUsuario rol, String nombre, String correo) {
		Instant ahora = Instant.now();
		Instant expira = ahora.plus(duracion);
		String token = Jwts.builder()
				.subject(run)
				.claim(CLAIM_ROL, rol.name())
				.claim(CLAIM_NOMBRE, nombre)
				.claim(CLAIM_CORREO, correo)
				.issuedAt(Date.from(ahora))
				.expiration(Date.from(expira))
				.signWith(clave)
				.compact();
		return new TokenEmitido(token, expira);
	}

	/** Devuelve los datos del token si la firma es válida y no expiró; vacío en cualquier otro caso. */
	public Optional<UsuarioToken> validar(String token) {
		try {
			Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
			TipoUsuario rol = TipoUsuario.valueOf(claims.get(CLAIM_ROL, String.class));
			return Optional.of(new UsuarioToken(claims.getSubject(), rol,
					claims.get(CLAIM_NOMBRE, String.class), claims.get(CLAIM_CORREO, String.class)));
		} catch (JwtException | IllegalArgumentException | NullPointerException e) {
			return Optional.empty();
		}
	}

	public record TokenEmitido(String token, Instant expiraEn) {
	}
}
