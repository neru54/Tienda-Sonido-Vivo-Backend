package cl.sonidovivo.catalogo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.sonidovivo.catalogo.dto.CategoriaRequest;
import cl.sonidovivo.catalogo.dto.CategoriaResponse;
import cl.sonidovivo.catalogo.model.Categoria;
import cl.sonidovivo.catalogo.repository.CategoriaRepository;
import cl.sonidovivo.catalogo.repository.ProductoRepository;
import cl.sonidovivo.common.exception.ConflictoException;
import cl.sonidovivo.common.exception.RecursoNoEncontradoException;
import cl.sonidovivo.common.exception.ReglaNegocioException;
import cl.sonidovivo.common.util.Textos;

/**
 * Reglas de categorías (las mismas del monolito):
 *  - El id se deriva del nombre (slug) y no cambia al renombrar.
 *  - No puede haber dos categorías con el mismo nombre.
 *  - No se puede eliminar una categoría que tiene productos (ambas tablas viven en este servicio).
 */
@Service
@Transactional(readOnly = true)
public class CategoriaService {

	static final String COLOR_POR_DEFECTO = "#2A241C";

	private final CategoriaRepository categorias;
	private final ProductoRepository productos;

	public CategoriaService(CategoriaRepository categorias, ProductoRepository productos) {
		this.categorias = categorias;
		this.productos = productos;
	}

	public List<CategoriaResponse> listar() {
		return categorias.findAll().stream()
				.map(c -> CategoriaResponse.desde(c, productos.countByCategoriaId(c.getId())))
				.toList();
	}

	public CategoriaResponse obtener(String id) {
		Categoria c = buscar(id);
		return CategoriaResponse.desde(c, productos.countByCategoriaId(id));
	}

	@Transactional
	public CategoriaResponse crear(CategoriaRequest datos) {
		String nombre = datos.nombre().trim();
		String id = Textos.slug(nombre);
		if (id.isEmpty()) throw new ReglaNegocioException("El nombre debe contener letras o números.");
		if (categorias.existsByNombreIgnoreCase(nombre) || categorias.existsById(id)) {
			throw new ConflictoException("Ya existe una categoría con ese nombre.");
		}
		String color = Textos.limpio(datos.color());
		Categoria nueva = new Categoria(id, nombre, Textos.limpio(datos.descripcion()), color == null ? COLOR_POR_DEFECTO : color);
		return CategoriaResponse.desde(categorias.save(nueva), 0);
	}

	@Transactional
	public CategoriaResponse actualizar(String id, CategoriaRequest datos) {
		Categoria c = buscar(id);
		String nombre = datos.nombre().trim();
		categorias.findByNombreIgnoreCase(nombre)
				.filter(otra -> !otra.getId().equals(id))
				.ifPresent(otra -> {
					throw new ConflictoException("Ya existe una categoría con ese nombre.");
				});
		c.setNombre(nombre);
		c.setDescripcion(Textos.limpio(datos.descripcion()));
		String color = Textos.limpio(datos.color());
		if (color != null) c.setColor(color);
		return CategoriaResponse.desde(c, productos.countByCategoriaId(id));
	}

	@Transactional
	public void eliminar(String id) {
		Categoria c = buscar(id);
		if (productos.countByCategoriaId(id) > 0) {
			throw new ConflictoException("No se puede eliminar \"" + c.getNombre() + "\" porque tiene productos asociados.");
		}
		categorias.delete(c);
	}

	Categoria buscar(String id) {
		return categorias.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("No se encontró la categoría."));
	}
}
