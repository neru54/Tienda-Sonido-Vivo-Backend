package cl.sonidovivo.common.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Acceso a quién hace la petición actual (vacío si es un visitante sin token). */
public class SesionActual {

	public Optional<UsuarioToken> usuario() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof UsuarioToken u) return Optional.of(u);
		return Optional.empty();
	}

	public Optional<String> run() {
		return usuario().map(UsuarioToken::run);
	}

	/** true si es Administrador o Vendedor (personal de la tienda). */
	public boolean esPersonal() {
		return usuario().map(UsuarioToken::esPersonal).orElse(false);
	}
}
