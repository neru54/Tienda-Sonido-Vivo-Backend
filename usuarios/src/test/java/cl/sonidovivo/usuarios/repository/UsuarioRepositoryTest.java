package cl.sonidovivo.usuarios.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import cl.sonidovivo.common.security.TipoUsuario;
import cl.sonidovivo.usuarios.model.Usuario;

/** Consultas derivadas del repositorio y la restricción UNIQUE del correo, contra H2 real. */
@DataJpaTest
class UsuarioRepositoryTest {

	@Autowired
	UsuarioRepository repo;

	static Usuario usuario(String run, String correo) {
		Usuario u = new Usuario();
		u.setRun(run);
		u.setNombre("N");
		u.setApellidos("A");
		u.setCorreo(correo);
		u.setPassword("$2a$hash");
		u.setTipoUsuario(TipoUsuario.CLIENTE);
		return u;
	}

	@Test
	void buscaPorCorreoSinDistinguirMayusculasYOrdenaPorRun() {
		repo.save(usuario("33333333-3", "camila.rojas@gmail.com"));
		repo.save(usuario("11111111-1", "maria.perez@gmail.com"));

		assertThat(repo.findByCorreoIgnoreCase("CAMILA.Rojas@gmail.com")).isPresent();
		assertThat(repo.existsByCorreoIgnoreCase("MARIA.PEREZ@GMAIL.COM")).isTrue();
		assertThat(repo.existsByCorreoIgnoreCaseAndRunNot("maria.perez@gmail.com", "11111111-1")).isFalse();
		assertThat(repo.existsByCorreoIgnoreCaseAndRunNot("maria.perez@gmail.com", "33333333-3")).isTrue();
		assertThat(repo.findAllByOrderByRunAsc()).extracting(Usuario::getRun).containsExactly("11111111-1", "33333333-3");
	}

	@Test
	void elCorreoEsUnicoEnLaBase() {
		repo.saveAndFlush(usuario("11111111-1", "repetido@gmail.com"));
		assertThatThrownBy(() -> repo.saveAndFlush(usuario("22222222-2", "repetido@gmail.com")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
