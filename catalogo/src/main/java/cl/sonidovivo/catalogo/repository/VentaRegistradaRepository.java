package cl.sonidovivo.catalogo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cl.sonidovivo.catalogo.model.VentaRegistrada;

@Repository
public interface VentaRegistradaRepository extends JpaRepository<VentaRegistrada, String> {

	/** true si el producto aparece en algún comprobante de venta (entonces no se puede eliminar). */
	@Query("select count(v) > 0 from VentaRegistrada v join v.lineas l where l.codigo = :codigo")
	boolean existeVentaDelProducto(@Param("codigo") String codigo);
}
