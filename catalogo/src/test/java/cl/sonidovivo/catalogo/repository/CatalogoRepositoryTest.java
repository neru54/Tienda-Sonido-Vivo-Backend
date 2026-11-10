package cl.sonidovivo.catalogo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import cl.sonidovivo.catalogo.model.Categoria;
import cl.sonidovivo.catalogo.model.Producto;
import cl.sonidovivo.catalogo.model.VentaLinea;
import cl.sonidovivo.catalogo.model.VentaRegistrada;

/** Consultas JPQL del catálogo contra H2 real: críticos, suma de stock, conteo por categoría y ventas. */
@DataJpaTest
class CatalogoRepositoryTest {

	@Autowired
	CategoriaRepository categorias;
	@Autowired
	ProductoRepository productos;
	@Autowired
	VentaRegistradaRepository ventas;

	@BeforeEach
	void preparar() {
		Categoria c = categorias.save(new Categoria("accesorios", "Accesorios", null, null));
		productos.save(producto("AC001", c, 2, 3)); // crítico (2 <= 3)
		productos.save(producto("AC002", c, 10, 3));
		productos.save(producto("AC003", c, 0, 3)); // crítico
	}

	static Producto producto(String codigo, Categoria c, int stock, int critico) {
		Producto p = new Producto();
		p.setCodigo(codigo);
		p.setNombre("Producto " + codigo);
		p.setCategoria(c);
		p.setPrecio(1000);
		p.setStock(stock);
		p.setStockCritico(critico);
		return p;
	}

	@Test
	void criticosOrdenadosPorStockYSumaDeStock() {
		assertThat(productos.findCriticos()).extracting(Producto::getCodigo).containsExactly("AC003", "AC001");
		assertThat(productos.sumarStock()).isEqualTo(12L);
		assertThat(productos.countByCategoriaId("accesorios")).isEqualTo(3L);
		assertThat(categorias.existsByNombreIgnoreCase("ACCESORIOS")).isTrue();
	}

	@Test
	void detectaSiUnProductoTieneVentas() {
		VentaRegistrada v = new VentaRegistrada("1001", Instant.now());
		v.getLineas().add(new VentaLinea("AC001", "Producto AC001", "Accesorios", 1, 1000));
		ventas.saveAndFlush(v);

		assertThat(ventas.existeVentaDelProducto("AC001")).isTrue();
		assertThat(ventas.existeVentaDelProducto("AC002")).isFalse();
		assertThat(ventas.findById("1001").orElseThrow().getLineas()).hasSize(1);
	}

	@Test
	void bloqueoParaActualizarDevuelveElProducto() {
		assertThat(productos.findByCodigoParaActualizar("AC002")).get().extracting(Producto::getStock).isEqualTo(10);
		assertThat(productos.findByCodigoParaActualizar("ZZ999")).isEmpty();
	}
}
