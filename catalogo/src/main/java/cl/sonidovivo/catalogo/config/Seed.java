package cl.sonidovivo.catalogo.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.sonidovivo.catalogo.model.Categoria;
import cl.sonidovivo.catalogo.model.Producto;
import cl.sonidovivo.catalogo.repository.CategoriaRepository;
import cl.sonidovivo.catalogo.repository.ProductoRepository;

/**
 * Carga las 10 categorías y 51 productos de la tienda (JSON en
 * resources/semilla) cuando las tablas están vacías. Se desactiva con SEMILLA=false.
 */
@Component
@ConditionalOnProperty(name = "app.semilla.habilitada", havingValue = "true")
public class Seed implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(Seed.class);

	private final CategoriaRepository categorias;
	private final ProductoRepository productos;
	private final ObjectMapper mapper;

	public Seed(CategoriaRepository categorias, ProductoRepository productos, ObjectMapper mapper) {
		this.categorias = categorias;
		this.productos = productos;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public void run(String... args) throws IOException {
		if (categorias.count() == 0) {
			List<Map<String, String>> datos = leer("semilla/categorias.json");
			datos.forEach(c -> categorias.save(new Categoria(c.get("id"), c.get("nombre"), c.get("descripcion"), c.get("color"))));
			log.info("Carga inicial: {} categorías", datos.size());
		}
		if (productos.count() == 0) {
			Map<String, Categoria> porNombre = new HashMap<>();
			categorias.findAll().forEach(c -> porNombre.put(c.getNombre(), c));
			List<Map<String, Object>> datos = leer("semilla/productos.json");
			for (Map<String, Object> d : datos) {
				Producto p = new Producto();
				p.setCodigo((String) d.get("codigo"));
				p.setNombre((String) d.get("nombre"));
				p.setCategoria(porNombre.get((String) d.get("categoria")));
				p.setMarca((String) d.get("marca"));
				p.setModelo((String) d.get("modelo"));
				p.setDescripcion((String) d.get("descripcion"));
				p.setImagen((String) d.get("imagen"));
				p.setPrecio(((Number) d.get("precio")).intValue());
				p.setStock(((Number) d.get("stock")).intValue());
				p.setStockCritico(((Number) d.get("stockCritico")).intValue());
				p.setOferta(Boolean.TRUE.equals(d.get("oferta")));
				p.setPrecioOferta(d.get("precioOferta") == null ? null : ((Number) d.get("precioOferta")).intValue());
				productos.save(p);
			}
			log.info("Carga inicial: {} productos", datos.size());
		}
	}

	private <T> List<T> leer(String ruta) throws IOException {
		try (InputStream in = new ClassPathResource(ruta).getInputStream()) {
			return mapper.readValue(in, new TypeReference<List<T>>() {
			});
		}
	}
}
