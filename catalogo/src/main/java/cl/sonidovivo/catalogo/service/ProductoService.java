package cl.sonidovivo.catalogo.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.sonidovivo.catalogo.dto.ProductoRequest;
import cl.sonidovivo.catalogo.dto.ProductoResponse;
import cl.sonidovivo.catalogo.model.Categoria;
import cl.sonidovivo.catalogo.model.Producto;
import cl.sonidovivo.catalogo.repository.CategoriaRepository;
import cl.sonidovivo.catalogo.repository.ProductoRepository;
import cl.sonidovivo.catalogo.repository.VentaRegistradaRepository;
import cl.sonidovivo.common.exception.ConflictoException;
import cl.sonidovivo.common.exception.RecursoNoEncontradoException;
import cl.sonidovivo.common.exception.ReglaNegocioException;
import cl.sonidovivo.common.util.Textos;

/**
 * Reglas de productos:
 *  - El código es único y no se cambia al editar.
 *  - La categoría debe existir.
 *  - Si está en oferta, el precio de oferta es obligatorio y no puede superar el precio normal.
 *  - Stock crítico por defecto = 3.
 *  - No se puede eliminar un producto que ya se vendió. Para saberlo
 *    se mira el comprobante propio de catalogo (VENTA_REGISTRADA), porque no puede leer el esquema de pedidos.
 */
@Service
@Transactional(readOnly = true)
public class ProductoService {

	private final ProductoRepository productos;
	private final CategoriaRepository categorias;
	private final VentaRegistradaRepository ventas;

	public ProductoService(ProductoRepository productos, CategoriaRepository categorias, VentaRegistradaRepository ventas) {
		this.productos = productos;
		this.categorias = categorias;
		this.ventas = ventas;
	}

	/** Catálogo con filtros opcionales: categoría (id), texto (nombre, marca, modelo o código) y solo ofertas. */
	public List<ProductoResponse> listar(String categoriaId, String texto, Boolean soloOfertas) {
		String busca = texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
		List<Producto> base = categoriaId == null || categoriaId.isBlank()
				? productos.findAllByOrderByCodigoAsc()
				: productos.findByCategoriaIdOrderByCodigoAsc(categoriaId);
		return base.stream()
				.filter(p -> !Boolean.TRUE.equals(soloOfertas) || p.isOferta())
				.filter(p -> busca.isEmpty() || coincide(p, busca))
				.map(ProductoResponse::desde)
				.toList();
	}

	public List<ProductoResponse> criticos() {
		return productos.findCriticos().stream().map(ProductoResponse::desde).toList();
	}

	public ProductoResponse obtener(String codigo) {
		return ProductoResponse.desde(buscar(codigo));
	}

	@Transactional
	public ProductoResponse crear(ProductoRequest datos) {
		String codigo = Textos.normalizarCodigo(datos.codigo());
		if (productos.existsById(codigo)) throw new ConflictoException("Ya existe un producto con ese código.");
		Producto p = new Producto();
		p.setCodigo(codigo);
		aplicar(p, datos);
		return ProductoResponse.desde(productos.save(p));
	}

	@Transactional
	public ProductoResponse actualizar(String codigo, ProductoRequest datos) {
		Producto p = buscar(codigo);
		aplicar(p, datos);
		return ProductoResponse.desde(p);
	}

	@Transactional
	public void eliminar(String codigo) {
		Producto p = buscar(codigo);
		if (ventas.existeVentaDelProducto(p.getCodigo())) {
			throw new ConflictoException("No se puede eliminar el producto porque tiene ventas registradas. Puedes dejar su stock en 0.");
		}
		productos.delete(p);
	}

	Producto buscar(String codigo) {
		return productos.findById(Textos.normalizarCodigo(codigo))
				.orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el producto solicitado."));
	}

	private void aplicar(Producto p, ProductoRequest d) {
		Categoria categoria = categorias.findById(d.categoriaId())
				.orElseThrow(() -> new ReglaNegocioException("La categoría seleccionada no existe."));
		if (d.oferta()) {
			if (d.precioOferta() == null) throw new ReglaNegocioException("Ingresa el precio de oferta.");
			if (d.precioOferta() > d.precio()) throw new ReglaNegocioException("El precio de oferta no puede ser mayor al precio normal.");
		}
		p.setNombre(d.nombre().trim());
		p.setCategoria(categoria);
		p.setMarca(Textos.limpio(d.marca()));
		p.setModelo(Textos.limpio(d.modelo()));
		p.setDescripcion(Textos.limpio(d.descripcion()));
		p.setImagen(Textos.limpio(d.imagen()));
		p.setPrecio(d.precio());
		p.setStock(d.stock());
		p.setStockCritico(d.stockCritico() == null ? Producto.STOCK_CRITICO_POR_DEFECTO : d.stockCritico());
		p.setOferta(d.oferta());
		p.setPrecioOferta(d.oferta() ? d.precioOferta() : null);
	}

	private static boolean coincide(Producto p, String busca) {
		for (String campo : new String[] { p.getNombre(), p.getMarca(), p.getModelo(), p.getCodigo() }) {
			if (campo != null && campo.toLowerCase(Locale.ROOT).contains(busca)) return true;
		}
		return false;
	}
}
