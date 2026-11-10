package cl.sonidovivo.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/** FiltroJwt y ClaveInternaFiltro: qué identidad queda en el SecurityContext según los encabezados. */
class FiltrosTest {

	private final JwtUtil jwt = new JwtUtil(JwtUtilTest.SECRETO, 60);

	@AfterEach
	void limpiar() {
		SecurityContextHolder.clearContext();
	}

	private static Authentication autenticacion() {
		return SecurityContextHolder.getContext().getAuthentication();
	}

	@Test
	void tokenValidoDejaAlUsuarioConSuRol() throws Exception {
		var req = new MockHttpServletRequest("GET", "/api/v1/boletas");
		String token = jwt.generar("11111111-1", TipoUsuario.ADMINISTRADOR, "María", "m@gmail.com").token();
		req.addHeader("Authorization", "Bearer " + token);

		new FiltroJwt(jwt).doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(autenticacion().getPrincipal()).isInstanceOf(UsuarioToken.class);
		assertThat(autenticacion().getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMINISTRADOR");
	}

	@Test
	void sinTokenOTokenInvalidoSigueAnonimo() throws Exception {
		var chain = new MockFilterChain();
		var req = new MockHttpServletRequest("GET", "/api/v1/productos");
		req.addHeader("Authorization", "Bearer basura");
		new FiltroJwt(jwt).doFilter(req, new MockHttpServletResponse(), chain);

		assertThat(autenticacion()).isNull();
		assertThat(chain.getRequest()).isNotNull(); // la petición continúa: las reglas deciden 401 o público
	}

	@Test
	void claveInternaCorrectaDaRolDeServicio() throws Exception {
		var req = new MockHttpServletRequest("POST", "/internal/ventas");
		req.setRequestURI("/internal/ventas");
		req.addHeader(ClaveInternaFiltro.ENCABEZADO, "clave-secreta");

		new ClaveInternaFiltro("clave-secreta").doFilter(req, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(autenticacion().getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.containsExactly("ROLE_" + ClaveInternaFiltro.ROL);
	}

	@Test
	void claveInternaIncorrectaVaciaOFueraDeInternalNoAutentica() throws Exception {
		var mala = new MockHttpServletRequest("POST", "/internal/ventas");
		mala.setRequestURI("/internal/ventas");
		mala.addHeader(ClaveInternaFiltro.ENCABEZADO, "otra");
		new ClaveInternaFiltro("clave-secreta").doFilter(mala, new MockHttpServletResponse(), new MockFilterChain());
		assertThat(autenticacion()).isNull();

		// Si el servidor no tiene clave configurada, ni siquiera una clave vacía sirve
		var vacia = new MockHttpServletRequest("POST", "/internal/ventas");
		vacia.setRequestURI("/internal/ventas");
		vacia.addHeader(ClaveInternaFiltro.ENCABEZADO, "");
		new ClaveInternaFiltro("").doFilter(vacia, new MockHttpServletResponse(), new MockFilterChain());
		assertThat(autenticacion()).isNull();

		// La clave solo se considera en /internal/**
		var publica = new MockHttpServletRequest("GET", "/api/v1/productos");
		publica.setRequestURI("/api/v1/productos");
		publica.addHeader(ClaveInternaFiltro.ENCABEZADO, "clave-secreta");
		new ClaveInternaFiltro("clave-secreta").doFilter(publica, new MockHttpServletResponse(), new MockFilterChain());
		assertThat(autenticacion()).isNull();
	}
}
