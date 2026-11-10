package cl.sonidovivo.catalogo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cl.sonidovivo.catalogo.model.Producto;
import jakarta.persistence.LockModeType;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, String> {

	List<Producto> findAllByOrderByCodigoAsc();

	List<Producto> findByCategoriaIdOrderByCodigoAsc(String categoriaId);

	long countByCategoriaId(String categoriaId);

	@Query("select p from Producto p where p.stock <= p.stockCritico order by p.stock asc, p.codigo asc")
	List<Producto> findCriticos();

	/** Unidades totales en bodega (dato del reporte de ventas que pide pedidos). */
	@Query("select coalesce(sum(p.stock), 0) from Producto p")
	long sumarStock();

	/**
	 * Lee el producto bloqueando su fila hasta que termine la transacción (SELECT ... FOR UPDATE).
	 * Si dos clientes compran la última unidad a la vez, el segundo espera y luego ve el stock
	 * ya descontado: nunca se vende más de lo que hay.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Producto p where p.codigo = :codigo")
	Optional<Producto> findByCodigoParaActualizar(@Param("codigo") String codigo);
}
