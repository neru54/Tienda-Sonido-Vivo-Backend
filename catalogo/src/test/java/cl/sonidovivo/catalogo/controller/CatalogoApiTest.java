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

	static final String PRODUCTO = """
			{"codigo":"AC099","nombre":"Cable de prueba","categoriaId":"accesorios","precio":9990,"stock":10,"oferta":false}
			""";

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
}
