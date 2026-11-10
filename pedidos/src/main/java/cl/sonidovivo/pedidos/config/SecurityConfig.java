package cl.sonidovivo.pedidos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.sonidovivo.common.security.JwtUtil;
import cl.sonidovivo.common.security.SeguridadBase;

/**
 * Reglas de acceso del microservicio pedidos (la parte común está en common/SeguridadBase).
 *
 *  Público .................. POST /boletas (se puede comprar sin cuenta)
 *  Administrador y Vendedor . GET /boletas (lista de órdenes)
 *  Cualquier autenticado .... GET /boletas/{id} (el servicio revisa que un cliente solo vea las suyas)
 *  Administrador y Cliente .. GET /usuarios/me/boletas (mis compras). El Vendedor no compra: 403
 *  Solo Administrador ....... GET /usuarios/{run}/boletas y /reportes/**
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final String ADMIN = "ADMINISTRADOR";
	private static final String VENDEDOR = "VENDEDOR";
	private static final String CLIENTE = "CLIENTE";
	private static final String API = "/api/v1";

	@Bean
	SecurityFilterChain cadenaSeguridad(HttpSecurity http, JwtUtil jwt, ObjectMapper mapper) throws Exception {
		// pedidos no recibe llamadas internas (solo las hace hacia catalogo): "" = ninguna clave aceptada
		SeguridadBase.aplicar(http, jwt, "", mapper)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/h2-console/**", "/error").permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

						.requestMatchers(HttpMethod.POST, API + "/boletas").permitAll()
						.requestMatchers(HttpMethod.GET, API + "/boletas").hasAnyRole(ADMIN, VENDEDOR)
						.requestMatchers(HttpMethod.GET, API + "/boletas/*").authenticated()

						.requestMatchers(HttpMethod.GET, API + "/usuarios/me/boletas").hasAnyRole(ADMIN, CLIENTE)
						.requestMatchers(HttpMethod.GET, API + "/usuarios/*/boletas").hasRole(ADMIN)

						.requestMatchers(API + "/reportes/**").hasRole(ADMIN)

						.anyRequest().denyAll());
		return http.build();
	}
}
