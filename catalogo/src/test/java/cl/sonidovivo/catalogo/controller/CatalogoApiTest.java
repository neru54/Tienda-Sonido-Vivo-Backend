package cl.sonidovivo.catalogo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import cl.sonidovivo.common.security.ClaveInternaFiltro;
import cl.sonidovivo.common.security.JwtUtil;
import cl.sonidovivo.common.security.TipoUsuario;

/**
 * HTTP -> seguridad -> controller -> service -> JPA (H2) con la semilla real (10 categorías, 51 productos).
 * Los tokens se firman con el mismo JwtUtil y secreto que usaría el microservicio usuarios:
 * así se prueba que catalogo acepta un JWT que él no emitió.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.semilla.habilitada=true")
class CatalogoApiTest {

	static final String CLAVE = "clave-de-prueba"; // app.internal.api-key de src/test/resources

	@Autowired
	MockMvc mvc;
	@Autowired
	JwtUtil jwt;

	String token(TipoUsuario rol) {
		return jwt.generar("11111111-1", rol, "Prueba", "prueba@gmail.com").token();
	}

	MockHttpServletRequestBuilder como(TipoUsuario rol, MockHttpServletRequestBuilder r) {
		return r.header("Authorization", "Bearer " + token(rol));
	}

	static MockHttpServletRequestBuilder interno(MockHttpServletRequestBuilder r) {
		return r.header(ClaveInternaFiltro.ENCABEZADO, CLAVE);
	}

	static final String PRODUCTO = """
			{"codigo":"AC099","nombre":"Cable de prueba","categoriaId":"accesorios","precio":9990,"stock":10,"oferta":false}
			""";

	@Test
	void catalogoPublicoConFiltros() throws Exception {
		mvc.perform(get("/api/v1/productos")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(51));
		mvc.perform(get("/api/v1/categorias")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(10));
		mvc.perform(get("/api/v1/productos").param("oferta", "true")).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].oferta").value(true));
		mvc.perform(get("/api/v1/productos/ga001")).andExpect(status().isOk())
				.andExpect(jsonPath("$.codigo").value("GA001"))
				.andExpect(jsonPath("$.precioVigente").value(110490));
		mvc.perform(get("/api/v1/productos/NO-EXISTE")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("No se encontró el producto solicitado."));
	}

	@Test
	void vendedorSoloLeeProductos() throws Exception {
		mvc.perform(como(TipoUsuario.VENDEDOR, get("/api/v1/productos"))).andExpect(status().isOk());
		mvc.perform(como(TipoUsuario.VENDEDOR, get("/api/v1/productos/GA001"))).andExpect(status().isOk());
		mvc.perform(como(TipoUsuario.VENDEDOR, get("/api/v1/productos/criticos"))).andExpect(status().isForbidden());
		mvc.perform(como(TipoUsuario.VENDEDOR, post("/api/v1/productos")).contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
				.andExpect(status().isForbidden());
		mvc.perform(como(TipoUsuario.VENDEDOR, delete("/api/v1/productos/GA001"))).andExpect(status().isForbidden());
		mvc.perform(como(TipoUsuario.VENDEDOR, post("/api/v1/categorias")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Nueva\"}")).andExpect(status().isForbidden());
	}

	@Test
	void criticosSoloAdministrador() throws Exception {
		mvc.perform(get("/api/v1/productos/criticos")).andExpect(status().isUnauthorized());
		mvc.perform(como(TipoUsuario.CLIENTE, get("/api/v1/productos/criticos"))).andExpect(status().isForbidden());
		mvc.perform(como(TipoUsuario.ADMINISTRADOR, get("/api/v1/productos/criticos"))).andExpect(status().isOk());
	}

	@Test
	void crudDeProductoDelAdministrador() throws Exception {
		mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
				.andExpect(status().isUnauthorized());
		mvc.perform(como(TipoUsuario.CLIENTE, post("/api/v1/productos")).contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
				.andExpect(status().isForbidden());

		mvc.perform(como(TipoUsuario.ADMINISTRADOR, post("/api/v1/productos")).contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.stockCritico").value(3));
		mvc.perform(como(TipoUsuario.ADMINISTRADOR, post("/api/v1/productos")).contentType(MediaType.APPLICATION_JSON).content(PRODUCTO))
				.andExpect(status().isConflict());
		mvc.perform(como(TipoUsuario.ADMINISTRADOR, post("/api/v1/productos")).contentType(MediaType.APPLICATION_JSON)
				.content(PRODUCTO.replace("9990", "-1"))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.precio").value("El precio no puede ser negativo."));

		mvc.perform(como(TipoUsuario.ADMINISTRADOR, put("/api/v1/productos/AC099")).contentType(MediaType.APPLICATION_JSON)
				.content(PRODUCTO.replace("\"oferta\":false", "\"oferta\":true,\"precioOferta\":7990"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.precioVigente").value(7990));
		mvc.perform(como(TipoUsuario.ADMINISTRADOR, delete("/api/v1/productos/AC099"))).andExpect(status().isNoContent());
		mvc.perform(get("/api/v1/productos/AC099")).andExpect(status().isNotFound());
	}

	@Test
	void categoriasCrudYNoEliminarConProductos() throws Exception {
		mvc.perform(como(TipoUsuario.ADMINISTRADOR, post("/api/v1/categorias")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"Vientos y Metales\",\"color\":\"#123456\"}"))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id").value("vientos-y-metales"));
		mvc.perform(como(TipoUsuario.ADMINISTRADOR, post("/api/v1/categorias")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"nombre\":\"vientos y metales\"}")).andExpect(status().isConflict());
		mvc.perform(como(TipoUsuario.ADMINISTRADOR, delete("/api/v1/categorias/vientos-y-metales"))).andExpect(status().isNoContent());
		mvc.perform(como(TipoUsuario.ADMINISTRADOR, delete("/api/v1/categorias/accesorios"))).andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("No se puede eliminar \"Accesorios\" porque tiene productos asociados."));
	}

	@Test
	void ventaInternaExigeClaveDeServicio() throws Exception {
		String venta = "{\"referencia\":\"9001\",\"items\":[{\"codigo\":\"GA001\",\"cantidad\":1}]}";
		mvc.perform(post("/internal/ventas").contentType(MediaType.APPLICATION_JSON).content(venta))
				.andExpect(status().isUnauthorized());
		mvc.perform(post("/internal/ventas").header(ClaveInternaFiltro.ENCABEZADO, "otra").contentType(MediaType.APPLICATION_JSON).content(venta))
				.andExpect(status().isUnauthorized());
		// Ni siquiera un Administrador con JWT puede descontar stock directamente
		mvc.perform(como(TipoUsuario.ADMINISTRADOR, post("/internal/ventas")).contentType(MediaType.APPLICATION_JSON).content(venta))
				.andExpect(status().isForbidden());
	}

	@Test
	void ventaInternaDescuentaUnaVezYBloqueaEliminarElProducto() throws Exception {
		// GA001 parte con stock 8 en la semilla
		String venta = "{\"referencia\":\"9002\",\"items\":[{\"codigo\":\"GA001\",\"cantidad\":2}]}";
		mvc.perform(interno(post("/internal/ventas")).contentType(MediaType.APPLICATION_JSON).content(venta))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lineas[0].precioUnitario").value(110490))
				.andExpect(jsonPath("$.lineas[0].categoria").value("Guitarras Acústicas"))
				.andExpect(jsonPath("$.subtotal").value(220980));
		mvc.perform(get("/api/v1/productos/GA001")).andExpect(jsonPath("$.stock").value(6));

		// Reintento: misma referencia, mismo resultado, stock intacto
		mvc.perform(interno(post("/internal/ventas")).contentType(MediaType.APPLICATION_JSON).content(venta))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.subtotal").value(220980));
		mvc.perform(get("/api/v1/productos/GA001")).andExpect(jsonPath("$.stock").value(6));

		mvc.perform(como(TipoUsuario.ADMINISTRADOR, delete("/api/v1/productos/GA001"))).andExpect(status().isConflict());
	}

	@Test
	void ventaSinStockSuficienteDa409YNoDescuentaNada() throws Exception {
		String venta = "{\"referencia\":\"9003\",\"items\":[{\"codigo\":\"GA001\",\"cantidad\":1},{\"codigo\":\"GA002\",\"cantidad\":999}]}";
		mvc.perform(interno(post("/internal/ventas")).contentType(MediaType.APPLICATION_JSON).content(venta))
				.andExpect(status().isConflict());
		mvc.perform(get("/api/v1/productos/GA001")).andExpect(jsonPath("$.stock").value(8));
		mvc.perform(interno(get("/internal/inventario"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.productos").value(51));
	}
}
