package cl.sonidovivo.common.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.sonidovivo.common.exception.ErrorRespuesta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Parte de la seguridad que es IGUAL en usuarios, catalogo y pedidos. Cada servicio, en su
 * config/SecurityConfig, llama a {@link #aplicar} y después escribe solo SUS reglas de acceso.
 *
 *  - Sin CSRF: la API no usa cookies; el token va en el encabezado Authorization.
 *  - Sin CORS: el navegador habla solo con el Gateway, que es quien aplica CORS.
 *  - STATELESS: no hay sesión de servidor; cada petición se autentica con su JWT.
 *  - 401 y 403 se responden con el mismo JSON {@link ErrorRespuesta} que el resto de errores.
 *  - Filtros: primero la clave interna (/internal/**) y luego el JWT.
 */
public final class SeguridadBase {

	private SeguridadBase() {
	}

	public static HttpSecurity aplicar(HttpSecurity http, JwtUtil jwt, String claveInterna, ObjectMapper mapper)
			throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.disable())
				.httpBasic(b -> b.disable())
				.formLogin(f -> f.disable())
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.headers(h -> h.frameOptions(f -> f.sameOrigin())) // consola H2 (solo perfil h2)
				.exceptionHandling(e -> e
						.authenticationEntryPoint((req, res, ex) -> escribirError(mapper, req, res, HttpStatus.UNAUTHORIZED,
								"Debes iniciar sesión para acceder a este recurso."))
						.accessDeniedHandler((req, res, ex) -> escribirError(mapper, req, res, HttpStatus.FORBIDDEN,
								"No tienes permisos para realizar esta acción.")))
				.addFilterBefore(new ClaveInternaFiltro(claveInterna), UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(new FiltroJwt(jwt), UsernamePasswordAuthenticationFilter.class);
	}

	/** Escribe un {@link ErrorRespuesta} directamente en la respuesta (fuera de los controladores). */
	public static void escribirError(ObjectMapper mapper, HttpServletRequest req, HttpServletResponse res,
			HttpStatus estado, String mensaje) throws IOException {
		res.setStatus(estado.value());
		res.setContentType(MediaType.APPLICATION_JSON_VALUE);
		res.setCharacterEncoding("UTF-8");
		mapper.writeValue(res.getOutputStream(),
				ErrorRespuesta.de(estado.value(), estado.getReasonPhrase(), mensaje, req.getRequestURI()));
	}
}
