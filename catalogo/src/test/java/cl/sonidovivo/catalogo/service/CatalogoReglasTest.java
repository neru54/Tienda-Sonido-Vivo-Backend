package cl.sonidovivo.catalogo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.sonidovivo.catalogo.dto.CategoriaRequest;
import cl.sonidovivo.catalogo.dto.ProductoRequest;
import cl.sonidovivo.catalogo.model.Categoria;
import cl.sonidovivo.catalogo.model.Producto;
import cl.sonidovivo.catalogo.repository.CategoriaRepository;
import cl.sonidovivo.catalogo.repository.ProductoRepository;
import cl.sonidovivo.catalogo.repository.VentaRegistradaRepository;
import cl.sonidovivo.common.exception.ConflictoException;
import cl.sonidovivo.common.exception.RecursoNoEncontradoException;
import cl.sonidovivo.common.exception.ReglaNegocioException;

/** Reglas de productos y categorías con los repositorios simulados (Mockito). */
@ExtendWith(MockitoExtension.class)
class CatalogoReglasTest {

	@Mock
	ProductoRepository productos;
	@Mock
	CategoriaRepository categorias;
	@Mock
	VentaRegistradaRepository ventas;

	ProductoService productoService;
	CategoriaService categoriaService;

	final Categoria accesorios = new Categoria("accesorios", "Accesorios", null, "#111111");

	@BeforeEach
	void preparar() {
		productoService = new ProductoService(productos, categorias, ventas);
		categoriaService = new CategoriaService(categorias, productos);
	}

	static ProductoRequest request(String codigo, boolean oferta, Integer precioOferta) {
		return new ProductoRequest(codigo, "Cable 3 m", "accesorios", "Fender", null, null, null, 9990, 10, null, oferta, precioOferta);
	}

	@Test
	void crearNormalizaCodigoYUsaStockCriticoPorDefecto() {
		when(categorias.findById("accesorios")).thenReturn(Optional.of(accesorios));
		when(productos.save(any(Producto.class))).thenAnswer(i -> i.getArgument(0));

		var p = productoService.crear(request(" ac010 ", false, 5000));

		assertThat(p.codigo()).isEqualTo("AC010");
		assertThat(p.stockCritico()).isEqualTo(3);
		assertThat(p.precioOferta()).isNull(); // sin oferta no se guarda precio de oferta
		assertThat(p.categoria()).isEqualTo("Accesorios");
	}

	@Test
	void reglasDeOfertaCategoriaYCodigoRepetido() {
		when(productos.existsById("AC001")).thenReturn(true);
		assertThatThrownBy(() -> productoService.crear(request("AC001", false, null)))
				.isInstanceOf(ConflictoException.class).hasMessage("Ya existe un producto con ese código.");

		when(categorias.findById("accesorios")).thenReturn(Optional.of(accesorios));
		assertThatThrownBy(() -> productoService.crear(request("AC010", true, null)))
				.isInstanceOf(ReglaNegocioException.class).hasMessage("Ingresa el precio de oferta.");
		assertThatThrownBy(() -> productoService.crear(request("AC010", true, 20000)))
				.isInstanceOf(ReglaNegocioException.class).hasMessage("El precio de oferta no puede ser mayor al precio normal.");

		var sinCategoria = new ProductoRequest("AC011", "X", "no-existe", null, null, null, null, 1, 1, null, false, null);
		assertThatThrownBy(() -> productoService.crear(sinCategoria))
				.isInstanceOf(ReglaNegocioException.class).hasMessage("La categoría seleccionada no existe.");
		verify(productos, never()).save(any());
	}

	@Test
	void noSeEliminaUnProductoConVentas() {
		Producto p = new Producto();
		p.setCodigo("GA001");
		when(productos.findById("GA001")).thenReturn(Optional.of(p));
		when(ventas.existeVentaDelProducto("GA001")).thenReturn(true);

		assertThatThrownBy(() -> productoService.eliminar("ga001")).isInstanceOf(ConflictoException.class)
				.hasMessageContaining("tiene ventas registradas");
		verify(productos, never()).delete(any());

		assertThatThrownBy(() -> productoService.obtener("NADA")).isInstanceOf(RecursoNoEncontradoException.class);
	}

	@Test
	void filtrosDeBusquedaYOfertas() {
		Producto a = new Producto();
		a.setCodigo("GA001"); a.setNombre("Guitarra Folk"); a.setMarca("Yamaha"); a.setCategoria(accesorios);
		a.setPrecio(100); a.setStock(5); a.setOferta(true); a.setPrecioOferta(80);
		Producto b = new Producto();
		b.setCodigo("BA001"); b.setNombre("Batería"); b.setMarca("Pearl"); b.setCategoria(accesorios);
		b.setPrecio(100); b.setStock(5);
		when(productos.findAllByOrderByCodigoAsc()).thenReturn(List.of(b, a));

		assertThat(productoService.listar(null, "yamaha", null)).extracting(x -> x.codigo()).containsExactly("GA001");
		assertThat(productoService.listar("", null, true)).extracting(x -> x.codigo()).containsExactly("GA001");
		assertThat(productoService.listar(null, null, null)).hasSize(2);
		assertThat(productoService.listar(null, "ga0", false).get(0).precioVigente()).isEqualTo(80);
	}

	@Test
	void categoriaUnicaYNoEliminableConProductos() {
		when(categorias.existsByNombreIgnoreCase("Accesorios")).thenReturn(true);
		assertThatThrownBy(() -> categoriaService.crear(new CategoriaRequest("Accesorios", null, null)))
				.isInstanceOf(ConflictoException.class).hasMessage("Ya existe una categoría con ese nombre.");
		assertThatThrownBy(() -> categoriaService.crear(new CategoriaRequest("!!!", null, null)))
				.isInstanceOf(ReglaNegocioException.class);

		when(categorias.save(any(Categoria.class))).thenAnswer(i -> i.getArgument(0));
		var nueva = categoriaService.crear(new CategoriaRequest("Vientos y Metales", null, ""));
		assertThat(nueva.id()).isEqualTo("vientos-y-metales");
		assertThat(nueva.color()).isEqualTo(CategoriaService.COLOR_POR_DEFECTO);

		when(categorias.findById("accesorios")).thenReturn(Optional.of(accesorios));
		when(productos.countByCategoriaId("accesorios")).thenReturn(4L);
		assertThatThrownBy(() -> categoriaService.eliminar("accesorios"))
				.isInstanceOf(ConflictoException.class).hasMessageContaining("tiene productos asociados");
		verify(categorias, never()).delete(any());
	}

	@Test
	void renombrarCategoriaAUnNombreDeOtraEsConflicto() {
		Categoria otra = new Categoria("baterias", "Baterías", null, null);
		when(categorias.findById("accesorios")).thenReturn(Optional.of(accesorios));
		when(categorias.findByNombreIgnoreCase("baterías")).thenReturn(Optional.of(otra));

		assertThatThrownBy(() -> categoriaService.actualizar("accesorios", new CategoriaRequest("baterías", null, null)))
				.isInstanceOf(ConflictoException.class);

		when(categorias.findByNombreIgnoreCase("Accesorios y Cables")).thenReturn(Optional.empty());
		var r = categoriaService.actualizar("accesorios", new CategoriaRequest("Accesorios y Cables", "d", null));
		assertThat(r.id()).isEqualTo("accesorios"); // el id no cambia al renombrar
		assertThat(r.color()).isEqualTo("#111111");
	}
}
