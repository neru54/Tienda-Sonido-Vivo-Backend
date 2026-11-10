package cl.sonidovivo.pedidos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.sonidovivo.pedidos.model.Boleta;

/**
 * Acceso a BOLETA. @EntityGraph("detalles") trae el detalle en la misma consulta (evita N+1 y
 * errores de carga perezosa, ya que open-in-view está desactivado).
 */
@Repository
public interface BoletaRepository extends JpaRepository<Boleta, Long> {

	@EntityGraph(attributePaths = "detalles")
	List<Boleta> findByEstadoOrderByFechaDescIdDesc(String estado);

	@EntityGraph(attributePaths = "detalles")
	List<Boleta> findByUsuarioRunAndEstadoOrderByFechaDescIdDesc(String usuarioRun, String estado);

	@EntityGraph(attributePaths = "detalles")
	Optional<Boleta> findByIdAndEstado(Long id, String estado);

	@EntityGraph(attributePaths = "detalles")
	Optional<Boleta> findByIdempotencyKey(String idempotencyKey);
}
