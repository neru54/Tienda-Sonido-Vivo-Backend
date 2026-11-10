package cl.sonidovivo.usuarios.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.sonidovivo.usuarios.model.Usuario;

/** Acceso a la tabla USUARIO. Spring Data genera la consulta a partir del nombre de cada método. */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, String> {

	Optional<Usuario> findByCorreoIgnoreCase(String correo);

	boolean existsByCorreoIgnoreCase(String correo);

	boolean existsByCorreoIgnoreCaseAndRunNot(String correo, String run);

	List<Usuario> findAllByOrderByRunAsc();
}
