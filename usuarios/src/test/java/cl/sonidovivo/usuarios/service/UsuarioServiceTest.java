package cl.sonidovivo.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import cl.sonidovivo.common.exception.ConflictoException;
import cl.sonidovivo.common.exception.RecursoNoEncontradoException;
import cl.sonidovivo.common.exception.ReglaNegocioException;
import cl.sonidovivo.common.security.JwtUtil;
import cl.sonidovivo.common.security.TipoUsuario;
import cl.sonidovivo.usuarios.dto.LoginRequest;
import cl.sonidovivo.usuarios.dto.PerfilRequest;
import cl.sonidovivo.usuarios.dto.RegistroRequest;
import cl.sonidovivo.usuarios.dto.UsuarioRequest;
import cl.sonidovivo.usuarios.model.Usuario;
import cl.sonidovivo.usuarios.repository.UsuarioRepository;

/** Reglas de negocio de usuarios y autenticación, con el repositorio simulado (Mockito). */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

	@Mock
	UsuarioRepository repo;

	// BCrypt real (factor bajo para que la prueba sea rápida): así se verifica que no se guarda texto plano
	final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
	UsuarioService servicio;
	AuthService auth;

	@BeforeEach
	void preparar() {
		servicio = new UsuarioService(repo, encoder);
		JwtUtil jwt = new JwtUtil("cHJ1ZWJhcy1zb25pZG8tdml2by1jbGF2ZS1kZS0yNTYtYml0cy1taW5pbW8tcGFyYS1obWFj", 60);
		auth = new AuthService(repo, servicio, encoder, jwt);
	}

	static Usuario usuario(String run, String correo, String password, TipoUsuario tipo) {
		Usuario u = new Usuario();
		u.setRun(run);
		u.setNombre("Nombre");
		u.setApellidos("Apellidos");
		u.setCorreo(correo);
		u.setPassword(password);
		u.setTipoUsuario(tipo);
		u.setRegion("R");
		u.setComuna("C");
		u.setDireccion("D");
		return u;
	}

	static UsuarioRequest request(String run, String correo, String password, TipoUsuario tipo) {
		return new UsuarioRequest(run, "Pedro", "Díaz", correo, password, null, tipo, "R", "C", "Calle 1");
	}

	@Test
	void crearGuardaRunNormalizadoYPasswordConBcrypt() {
		when(repo.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

		var creado = servicio.crear(request("10000013-k", "pedro@duoc.cl", "clave1", TipoUsuario.VENDEDOR));

		assertThat(creado.run()).isEqualTo("10000013-K");
		assertThat(creado.tipoUsuario()).isEqualTo(TipoUsuario.VENDEDOR);
		verify(repo).save(org.mockito.ArgumentMatchers.argThat(u ->
				!u.getPassword().equals("clave1") && encoder.matches("clave1", u.getPassword())));
	}

	@Test
	void crearRechazaRunInvalidoCorreoORunRepetidosYPasswordMala() {
		assertThatThrownBy(() -> servicio.crear(request("12345678-9", "a@gmail.com", "clave1", TipoUsuario.CLIENTE)))
				.isInstanceOf(ReglaNegocioException.class).hasMessageContaining("RUN");

		when(repo.existsByCorreoIgnoreCase("a@gmail.com")).thenReturn(true);
		assertThatThrownBy(() -> servicio.crear(request("12345678-5", "a@gmail.com", "clave1", TipoUsuario.CLIENTE)))
				.isInstanceOf(ConflictoException.class).hasMessage("Ya existe un usuario con ese correo.");

		when(repo.existsById("12345678-5")).thenReturn(true);
		assertThatThrownBy(() -> servicio.crear(request("12345678-5", "b@gmail.com", "clave1", TipoUsuario.CLIENTE)))
				.isInstanceOf(ConflictoException.class).hasMessage("Ya existe un usuario con ese RUN.");

		assertThatThrownBy(() -> servicio.crear(request("11111111-1", "c@gmail.com", "", TipoUsuario.CLIENTE)))
				.isInstanceOf(ReglaNegocioException.class).hasMessage("La contraseña es obligatoria.");
		assertThatThrownBy(() -> servicio.crear(request("11111111-1", "c@gmail.com", "abc", TipoUsuario.CLIENTE)))
				.isInstanceOf(ReglaNegocioException.class).hasMessageContaining("entre 4 y 10");
		verify(repo, never()).save(any());
	}

	@Test
	void actualizarConPasswordVaciaConservaLaAnterior() {
		Usuario u = usuario("22222222-2", "carlos@duoc.cl", encoder.encode("vend123"), TipoUsuario.VENDEDOR);
		String hashAnterior = u.getPassword();
		when(repo.findById("22222222-2")).thenReturn(Optional.of(u));

		servicio.actualizar("22222222-2", request("99999999-9", "carlos@duoc.cl", "", TipoUsuario.ADMINISTRADOR));

		assertThat(u.getPassword()).isEqualTo(hashAnterior);
		assertThat(u.getRun()).isEqualTo("22222222-2"); // el RUN del cuerpo se ignora
		assertThat(u.getTipoUsuario()).isEqualTo(TipoUsuario.ADMINISTRADOR);

		servicio.actualizar("22222222-2", request("22222222-2", "carlos@duoc.cl", "nueva12", TipoUsuario.VENDEDOR));
		assertThat(encoder.matches("nueva12", u.getPassword())).isTrue();
	}

	@Test
	void actualizarRechazaCorreoDeOtroUsuarioYUsuarioInexistente() {
		Usuario u = usuario("22222222-2", "carlos@duoc.cl", "x", TipoUsuario.VENDEDOR);
		when(repo.findById("22222222-2")).thenReturn(Optional.of(u));
		when(repo.existsByCorreoIgnoreCaseAndRunNot("maria.perez@gmail.com", "22222222-2")).thenReturn(true);

		assertThatThrownBy(() -> servicio.actualizar("22222222-2", request("22222222-2", "maria.perez@gmail.com", "", TipoUsuario.VENDEDOR)))
				.isInstanceOf(ConflictoException.class);
		assertThatThrownBy(() -> servicio.obtener("55555555-5")).isInstanceOf(RecursoNoEncontradoException.class);
	}

	@Test
	void perfilNoCambiaCorreoNiTipo() {
		Usuario u = usuario("33333333-3", "camila@gmail.com", "x", TipoUsuario.CLIENTE);
		when(repo.findById("33333333-3")).thenReturn(Optional.of(u));

		var r = servicio.actualizarPerfil("33333333-3", new PerfilRequest("Cami", "Rojas", null, null, "R2", "C2", "Nueva 1"));

		assertThat(r.nombre()).isEqualTo("Cami");
		assertThat(r.correo()).isEqualTo("camila@gmail.com");
		assertThat(r.tipoUsuario()).isEqualTo(TipoUsuario.CLIENTE);
	}

	@Test
	void nadiePuedeEliminarseASiMismo() {
		Usuario admin = usuario("11111111-1", "maria@gmail.com", "x", TipoUsuario.ADMINISTRADOR);
		Usuario otro = usuario("33333333-3", "camila@gmail.com", "x", TipoUsuario.CLIENTE);
		when(repo.findById("11111111-1")).thenReturn(Optional.of(admin));
		when(repo.findById("33333333-3")).thenReturn(Optional.of(otro));

		assertThatThrownBy(() -> servicio.eliminar("11111111-1", "11111111-1")).isInstanceOf(ConflictoException.class);
		verify(repo, never()).delete(admin);

		servicio.eliminar("33333333-3", "11111111-1");
		verify(repo).delete(otro);
	}

	@Test
	void loginConMensajeUnicoYRegistroSiempreCliente() {
		Usuario u = usuario("33333333-3", "camila.rojas@gmail.com", encoder.encode("cli123"), TipoUsuario.CLIENTE);
		when(repo.findByCorreoIgnoreCase("CAMILA.ROJAS@gmail.com")).thenReturn(Optional.of(u));
		when(repo.findByCorreoIgnoreCase("nadie@gmail.com")).thenReturn(Optional.empty());

		var ok = auth.login(new LoginRequest(" CAMILA.ROJAS@gmail.com ", "cli123"));
		assertThat(ok.token()).isNotBlank();
		assertThat(ok.tipo()).isEqualTo("Bearer");

		assertThatThrownBy(() -> auth.login(new LoginRequest("CAMILA.ROJAS@gmail.com", "mala")))
				.isInstanceOf(BadCredentialsException.class).hasMessage("Correo o contraseña incorrectos.");
		assertThatThrownBy(() -> auth.login(new LoginRequest("nadie@gmail.com", "x")))
				.isInstanceOf(BadCredentialsException.class).hasMessage("Correo o contraseña incorrectos.");

		when(repo.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
		var registrado = auth.registrar(new RegistroRequest("12345678-5", "Ana", "Soto", "ana@gmail.com", "abcd1234",
				"R", "C", "Calle 2", null));
		assertThat(registrado.usuario().tipoUsuario()).isEqualTo(TipoUsuario.CLIENTE);
	}
}
