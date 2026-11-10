package cl.sonidovivo.usuarios.controller;

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

import com.fasterxml.jackson.databind.JsonNode;
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

	static String usuarioJson(String run, String correo, String password, String tipo) {
		return """
				{"run":"%s","nombre":"Pedro","apellidos":"Díaz","correo":"%s","password":"%s",
				 "tipoUsuario":"%s","region":"Región de Valparaíso","comuna":"Quilpué","direccion":"Calle 1"}
				""".formatted(run, correo, password, tipo);
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
	void loginIncorrectoDa401YDatosVacios400() throws Exception {
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"correo\":\"maria.perez@gmail.com\",\"password\":\"mala\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos."));
		mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"correo\":\"\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void registroCreaSiempreClienteYDetectaDuplicados() throws Exception {
		String cuerpo = """
				{"run":"12345678-5","nombre":"Ana","apellidos":"Soto","correo":"ana.soto@gmail.com","password":"abcd1234",
				 "region":"Región de Valparaíso","comuna":"Quilpué","direccion":"Los Carrera 123","tipoUsuario":"Administrador"}
				""";
		mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.usuario.tipoUsuario").value("Cliente"));
		mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
				.andExpect(status().isConflict());
		mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content(cuerpo.replace("12345678-5", "44444444-4").replace("ana.soto@gmail.com", "ana@hotmail.com")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errores.correo").exists());
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

	@Test
	void perfilPropioParaClienteYAdminPeroNoVendedor() throws Exception {
		String perfil = """
				{"nombre":"Camila","apellidos":"Rojas Vera","password":"","region":"Región Metropolitana de Santiago",
				 "comuna":"Ñuñoa","direccion":"Irarrázaval 500"}
				""";
		String tokenCliente = cliente();
		mvc.perform(conToken(get("/api/v1/usuarios/me"), tokenCliente)).andExpect(status().isOk())
				.andExpect(jsonPath("$.correo").value(CLIENTE));
		mvc.perform(conToken(put("/api/v1/usuarios/me"), tokenCliente).contentType(MediaType.APPLICATION_JSON).content(perfil))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.comuna").value("Ñuñoa"));
		// La contraseña vacía conservó la anterior: el login con cli123 sigue funcionando
		cliente();

		mvc.perform(conToken(get("/api/v1/usuarios/me"), admin())).andExpect(status().isOk());
		mvc.perform(conToken(get("/api/v1/usuarios/me"), vendedor())).andExpect(status().isForbidden());
	}

	@Test
	void crudCompletoDelAdministrador() throws Exception {
		String t = admin();
		mvc.perform(conToken(post("/api/v1/usuarios"), t).contentType(MediaType.APPLICATION_JSON)
				.content(usuarioJson("44444444-4", "pedro.diaz@duoc.cl", "vend456", "Vendedor")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipoUsuario").value("Vendedor"));
		mvc.perform(conToken(post("/api/v1/usuarios"), t).contentType(MediaType.APPLICATION_JSON)
				.content(usuarioJson("55555555-5", "pedro.diaz@duoc.cl", "vend456", "Vendedor")))
				.andExpect(status().isConflict());

		mvc.perform(conToken(put("/api/v1/usuarios/44444444-4"), t).contentType(MediaType.APPLICATION_JSON)
				.content(usuarioJson("44444444-4", "pedro.diaz@duoc.cl", "", "Cliente")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipoUsuario").value("Cliente"));
		token("pedro.diaz@duoc.cl", "vend456"); // contraseña vacía = se conservó

		mvc.perform(conToken(get("/api/v1/usuarios/44444444-4"), t)).andExpect(status().isOk());
		mvc.perform(conToken(delete("/api/v1/usuarios/44444444-4"), t)).andExpect(status().isNoContent());
		mvc.perform(conToken(get("/api/v1/usuarios/44444444-4"), t)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.mensaje").value("No se encontró el usuario."));
	}

	@Test
	void elAdministradorNoPuedeEliminarseASiMismo() throws Exception {
		mvc.perform(conToken(delete("/api/v1/usuarios/11111111-1"), admin()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.mensaje").value("No puedes eliminar tu propio usuario mientras tienes la sesión iniciada."));
	}

	@Test
	void elTokenLlevaRunRolNombreYCorreo() throws Exception {
		String t = vendedor();
		String payload = new String(java.util.Base64.getUrlDecoder().decode(t.split("\\.")[1]));
		JsonNode claims = mapper.readTree(payload);
		org.assertj.core.api.Assertions.assertThat(claims.get("sub").asText()).isEqualTo("22222222-2");
		org.assertj.core.api.Assertions.assertThat(claims.get("rol").asText()).isEqualTo("VENDEDOR");
		org.assertj.core.api.Assertions.assertThat(claims.get("correo").asText()).isEqualTo(VENDEDOR);
		org.assertj.core.api.Assertions.assertThat(claims.has("password")).isFalse();
	}

	@Test
	void rutaDesconocidaQuedaDenegada() throws Exception {
		mvc.perform(conToken(get("/api/v1/otra-cosa"), admin())).andExpect(status().isForbidden());
	}
}
