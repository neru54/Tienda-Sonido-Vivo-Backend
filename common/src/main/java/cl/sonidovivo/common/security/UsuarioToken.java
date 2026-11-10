package cl.sonidovivo.common.security;

/**
 * Identidad que viaja en el token: es el "principal" de Spring Security en cada petición.
 * catalogo y pedidos la obtienen SOLO del JWT, sin consultar al microservicio usuarios.
 */
public record UsuarioToken(String run, TipoUsuario rol, String nombre, String correo) {

	/** Administrador o Vendedor (personal de la tienda). */
	public boolean esPersonal() {
		return rol == TipoUsuario.ADMINISTRADOR || rol == TipoUsuario.VENDEDOR;
	}
}
