package cl.sonidovivo.pedidos.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.sonidovivo.common.security.JwtUtil;
import cl.sonidovivo.common.security.TipoUsuario;
import cl.sonidovivo.pedidos.client.CatalogoCliente;
import cl.sonidovivo.pedidos.client.VentaCatalogo;

/**
 * HTTP -> seguridad -> controller -> service -> JPA (H2), con el cliente de catalogo SIMULADO (@MockitoBean).
 * Sin @Transactional: la compra confirma en transacciones propias (BoletaStore), así que cada prueba
 * trabaja con los números de boleta que ella misma crea.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PedidosApiTest {

	static final String RUN_CLIENTE = "33333333-3";

	@Autowired
	MockMvc mvc;
	@Autowired
	JwtUtil jwt;
	@Autowired
	ObjectMapper mapper;
	@MockitoBean
	CatalogoCliente catalogo;

	@BeforeEach
	void catalogoResponde() {
		reset(catalogo);
		// Catálogo simulado: GA001 a 110.490 (oferta), cualquier otro código a 10.000
		when(catalogo.registrarVenta(any())).thenAnswer(inv -> {
			VentaCatalogo.Peticion p = inv.getArgument(0);
			List<VentaCatalogo.Linea> lineas = p.items().stream().map(i -> {
				int precio = i.codigo().equals("GA001") ? 110_490 : 10_000;
				return new VentaCatalogo.Linea(i.codigo(), "Producto " + i.codigo(), "Guitarras Acústicas", i.cantidad(), precio, precio * i.cantidad());
			}).toList();
			return new VentaCatalogo.Respuesta(p.referencia(), Instant.now(), lineas, lineas.stream().mapToInt(VentaCatalogo.Linea::subtotal).sum());
		});
		when(catalogo.unidadesEnStock()).thenReturn(Optional.of(230L));
	}

	String token(String run, TipoUsuario rol) {
		return jwt.generar(run, rol, "Prueba", "prueba@gmail.com").token();
	}

	MockHttpServletRequestBuilder como(String run, TipoUsuario rol, MockHttpServletRequestBuilder r) {
		return r.header("Authorization", "Bearer " + token(run, rol));
	}

	MockHttpServletRequestBuilder cliente(MockHttpServletRequestBuilder r) { return como(RUN_CLIENTE, TipoUsuario.CLIENTE, r); }
	MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder r) { return como("11111111-1", TipoUsuario.ADMINISTRADOR, r); }
	MockHttpServletRequestBuilder vendedor(MockHttpServletRequestBuilder r) { return como("22222222-2", TipoUsuario.VENDEDOR, r); }

	static String compra(String cupon, String codigo, int cantidad) {
		return """
				{"cliente":{"nombre":"Camila","apellidos":"Rojas Vera","correo":"camila.rojas@gmail.com"},
				 "direccion":{"calle":"Av. Providencia 100","region":"Región Metropolitana de Santiago","comuna":"Providencia"},
				 "items":[{"codigo":"%s","cantidad":%d}],"cupon":%s}
				""".formatted(codigo, cantidad, cupon == null ? "null" : "\"" + cupon + "\"");
	}

	long comprar(MockHttpServletRequestBuilder peticion) throws Exception {
		String cuerpo = mvc.perform(peticion.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return mapper.readTree(cuerpo).get("id").asLong();
	}

	@Test
	void compraSinCuentaConCuponCalculaElTotalEnElServidor() throws Exception {
		mvc.perform(post("/api/v1/boletas").contentType(MediaType.APPLICATION_JSON).content(compra("SONIDOVIVO10", "GA001", 2)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(Matchers.greaterThanOrEqualTo(1001)))
				.andExpect(jsonPath("$.estado").value("Pagada"))
				.andExpect(jsonPath("$.cliente.run").doesNotExist())
				.andExpect(jsonPath("$.items[0].precio").value(110490))
				.andExpect(jsonPath("$.items[0].categoria").value("Guitarras Acústicas"))
				.andExpect(jsonPath("$.subtotal").value(220980))
				.andExpect(jsonPath("$.descuento").value(22098))
				.andExpect(jsonPath("$.total").value(198882));
	}

	@Test
	void compraConSesionQuedaEnElHistorialDelCliente() throws Exception {
		long id = comprar(cliente(post("/api/v1/boletas")).content(compra(null, "AC001", 1)));

		mvc.perform(cliente(get("/api/v1/usuarios/me/boletas"))).andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + id + ")].cliente.run").value(RUN_CLIENTE));
		mvc.perform(cliente(get("/api/v1/boletas/" + id))).andExpect(status().isOk());
		// Otro cliente no puede ver la boleta aunque adivine el número
		mvc.perform(como("44444444-4", TipoUsuario.CLIENTE, get("/api/v1/boletas/" + id))).andExpect(status().isForbidden());
		mvc.perform(admin(get("/api/v1/usuarios/" + RUN_CLIENTE + "/boletas"))).andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
		mvc.perform(cliente(get("/api/v1/usuarios/" + RUN_CLIENTE + "/boletas"))).andExpect(status().isForbidden());
	}

	@Test
	void permisosDelVendedorSoloListaYDetalleDeOrdenes() throws Exception {
		long id = comprar(post("/api/v1/boletas").content(compra(null, "AC002", 1)));

		mvc.perform(vendedor(get("/api/v1/boletas"))).andExpect(status().isOk());
		mvc.perform(vendedor(get("/api/v1/boletas/" + id))).andExpect(status().isOk());
		mvc.perform(vendedor(get("/api/v1/reportes/ventas"))).andExpect(status().isForbidden());
		mvc.perform(vendedor(get("/api/v1/usuarios/me/boletas"))).andExpect(status().isForbidden());
		mvc.perform(vendedor(get("/api/v1/usuarios/" + RUN_CLIENTE + "/boletas"))).andExpect(status().isForbidden());
	}
}
