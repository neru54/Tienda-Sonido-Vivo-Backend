package cl.sonidovivo.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** El JWT que emite usuarios debe ser aceptado por catalogo y pedidos SOLO si comparten el secreto. */
class JwtUtilTest {

	static final String SECRETO = "cHJ1ZWJhcy1zb25pZG8tdml2by1jbGF2ZS1kZS0yNTYtYml0cy1taW5pbW8tcGFyYS1obWFj";
	static final String OTRO_SECRETO = "b3RyYS1jbGF2ZS1kaXN0aW50YS1kZS0yNTYtYml0cy1wYXJhLXByb2JhciBsYSBmaXJtYQ==";

	@Test
	void unTokenEmitidoSeValidaConElMismoSecreto() {
		JwtUtil emisor = new JwtUtil(SECRETO, 60);
		JwtUtil validador = new JwtUtil(SECRETO, 60); // otro "servicio" con la misma variable JWT_SECRET

		var emitido = emisor.generar("11111111-1", TipoUsuario.VENDEDOR, "Carlos", "carlos.munoz@duoc.cl");
		var usuario = validador.validar(emitido.token());

		assertThat(usuario).isPresent();
		assertThat(usuario.get().run()).isEqualTo("11111111-1");
		assertThat(usuario.get().rol()).isEqualTo(TipoUsuario.VENDEDOR);
		assertThat(usuario.get().nombre()).isEqualTo("Carlos");
		assertThat(usuario.get().correo()).isEqualTo("carlos.munoz@duoc.cl");
		assertThat(usuario.get().esPersonal()).isTrue();
		assertThat(emitido.expiraEn()).isAfter(java.time.Instant.now());
	}

	@Test
	void otroSecretoAlteradoOVencidoNoSeAcepta() {
		JwtUtil emisor = new JwtUtil(SECRETO, 60);
		String token = emisor.generar("11111111-1", TipoUsuario.CLIENTE, "Camila", "c@gmail.com").token();

		assertThat(new JwtUtil(OTRO_SECRETO, 60).validar(token)).isEmpty();
		assertThat(emisor.validar(token.substring(0, token.length() - 3) + "abc")).isEmpty();
		assertThat(emisor.validar("no-es-un-jwt")).isEmpty();
		assertThat(emisor.validar("")).isEmpty();

		JwtUtil vencido = new JwtUtil(SECRETO, -1); // expiró hace un minuto
		assertThat(emisor.validar(vencido.generar("11111111-1", TipoUsuario.CLIENTE, "x", "x@gmail.com").token())).isEmpty();
	}

	@Test
	void secretoCortoSeRechazaAlIniciar() {
		// "corto" en Base64 = 5 bytes: HS256 exige al menos 32
		assertThatThrownBy(() -> new JwtUtil("Y29ydG8=", 60)).isInstanceOf(RuntimeException.class);
	}

	@Test
	void tipoUsuarioAceptaEtiquetaONombre() {
		assertThat(TipoUsuario.desde("Vendedor")).isEqualTo(TipoUsuario.VENDEDOR);
		assertThat(TipoUsuario.desde("ADMINISTRADOR")).isEqualTo(TipoUsuario.ADMINISTRADOR);
		assertThat(TipoUsuario.desde("cliente")).isEqualTo(TipoUsuario.CLIENTE);
		assertThat(TipoUsuario.desde(" ")).isNull();
		assertThat(TipoUsuario.CLIENTE.autoridad()).isEqualTo("ROLE_CLIENTE");
		assertThatThrownBy(() -> TipoUsuario.desde("Gerente")).isInstanceOf(IllegalArgumentException.class);
	}
}
