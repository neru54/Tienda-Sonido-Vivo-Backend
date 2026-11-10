package cl.sonidovivo.common.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Se ejecuta una vez por petición: si trae "Authorization: Bearer <jwt>" válido, deja al usuario
 * autenticado en el SecurityContext con su rol (ROLE_ADMINISTRADOR, ROLE_VENDEDOR o ROLE_CLIENTE).
 * Si no trae token o es inválido, la petición sigue como anónima y las reglas del SecurityConfig
 * de cada servicio deciden si puede pasar (rutas públicas) o recibe 401.
 *
 * No es @Component a propósito: si fuera un bean, Spring Boot lo registraría además como filtro
 * global de servlet. Lo crea {@link SeguridadBase} dentro de la cadena de Spring Security.
 */
public class FiltroJwt extends OncePerRequestFilter {

	private static final String PREFIJO = "Bearer ";

	private final JwtUtil jwt;

	public FiltroJwt(JwtUtil jwt) {
		this.jwt = jwt;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String encabezado = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (encabezado != null && encabezado.startsWith(PREFIJO)) {
			jwt.validar(encabezado.substring(PREFIJO.length()).trim()).ifPresent(u -> {
				var autenticacion = new UsernamePasswordAuthenticationToken(u, null,
						List.of(new SimpleGrantedAuthority(u.rol().autoridad())));
				SecurityContextHolder.getContext().setAuthentication(autenticacion);
			});
		}
		chain.doFilter(request, response);
	}
}
