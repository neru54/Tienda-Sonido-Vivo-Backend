package cl.sonidovivo.usuarios.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Prueba de punta a punta DENTRO del microservicio: HTTP -> seguridad -> controller -> service -> JPA (H2).
 * Usa la semilla real (3 usuarios demo) y tokens obtenidos con el login de verdad.
 * @Transactional deshace los cambios de cada prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.semilla.habilitada=true")
class UsuariosApiTest {

	static final String ADMIN = "maria.perez@gmail.com";
	static final String VENDEDOR = "carlos.munoz@duoc.cl";
	static final String CLIENTE = "camila.rojas@gmail.com";

	@Autowired
	MockMvc mvc;
	@Autowired
	ObjectMapper mapper;

	String token(String correo, String password) throws Exception {
		String cuerpo = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"" + correo + "\",\"password\":\"" + password + "\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return mapper.readTree(cuerpo).get("token").asText();
	}

	String admin() throws Exception { return token(ADMIN, "admin123"); }
	String vendedor() throws Exception { return token(VENDEDOR, "vend123"); }
	String cliente() throws Exception { return token(CLIENTE, "cli123"); }

	static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder r, String token) {
		return r.header("Authorization", "Bearer " + token);
	}

	@Test
	void loginDeLosTresRolesDevuelveTokenYTipo() throws Exception {
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"maria.perez@gmail.com\",\"password\":\"admin123\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipo").value("Bearer"))
				.andExpect(jsonPath("$.usuario.tipoUsuario").value("Administrador"))
				.andExpect(jsonPath("$.usuario.password").doesNotExist());
		vendedor();
		cliente();
	}

	@Test
	void authMeRequiereTokenYSirveAlVendedor() throws Exception {
		mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensaje").value("Debes iniciar sesión para acceder a este recurso."));
		mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer token-falso")).andExpect(status().isUnauthorized());
		mvc.perform(conToken(get("/api/v1/auth/me"), vendedor()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.run").value("22222222-2"))
				.andExpect(jsonPath("$.tipoUsuario").value("Vendedor"));
	}

	@Test
	void listaDeUsuariosSoloAdministrador() throws Exception {
		mvc.perform(get("/api/v1/usuarios")).andExpect(status().isUnauthorized());
		mvc.perform(conToken(get("/api/v1/usuarios"), cliente())).andExpect(status().isForbidden());
		mvc.perform(conToken(get("/api/v1/usuarios"), vendedor())).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.mensaje").value("No tienes permisos para realizar esta acción."));
		mvc.perform(conToken(get("/api/v1/usuarios"), admin())).andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].password").doesNotExist());
	}
}
