package cl.sonidovivo.common.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autentica las llamadas ENTRE microservicios (por ejemplo pedidos -> catalogo /internal/ventas).
 *
 * El servicio que llama envía el encabezado "X-Internal-Api-Key" con la variable INTERNAL_API_KEY.
 * Si coincide, la petición queda autenticada con el rol SERVICIO_INTERNO, que es el único que
 * pueden usar las rutas /internal/**. El navegador nunca conoce esta clave y, además, el Gateway
 * no publica /internal/**: son dos barreras independientes.
 *
 * La comparación usa MessageDigest.isEqual (tiempo constante) para no filtrar la clave por tiempos.
 * Si la clave no está configurada (vacía), ninguna llamada interna se acepta.
 */
public class ClaveInternaFiltro extends OncePerRequestFilter {

	public static final String ENCABEZADO = "X-Internal-Api-Key";
	public static final String ROL = "SERVICIO_INTERNO";
	public static final String PREFIJO_RUTA = "/internal/";

	private final byte[] clave;

	public ClaveInternaFiltro(String clave) {
		this.clave = clave == null ? new byte[0] : clave.getBytes(StandardCharsets.UTF_8);
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith(PREFIJO_RUTA);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String recibida = request.getHeader(ENCABEZADO);
		if (clave.length > 0 && recibida != null
				&& MessageDigest.isEqual(clave, recibida.getBytes(StandardCharsets.UTF_8))) {
			var autenticacion = new UsernamePasswordAuthenticationToken(ROL, null,
					List.of(new SimpleGrantedAuthority("ROLE_" + ROL)));
			SecurityContextHolder.getContext().setAuthentication(autenticacion);
		}
		chain.doFilter(request, response);
	}
}
