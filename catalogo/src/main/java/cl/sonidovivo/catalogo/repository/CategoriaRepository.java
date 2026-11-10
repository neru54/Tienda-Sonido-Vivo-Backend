package cl.sonidovivo.catalogo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.sonidovivo.catalogo.model.Categoria;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, String> {

	Optional<Categoria> findByNombreIgnoreCase(String nombre);

	boolean existsByNombreIgnoreCase(String nombre);
}
