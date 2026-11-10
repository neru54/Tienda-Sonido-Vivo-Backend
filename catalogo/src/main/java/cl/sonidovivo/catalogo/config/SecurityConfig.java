package cl.sonidovivo.catalogo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.sonidovivo.common.security.ClaveInternaFiltro;
import cl.sonidovivo.common.security.JwtUtil;
import cl.sonidovivo.common.security.SeguridadBase;

/**
 * Reglas de acceso del microservicio catalogo (la parte común está en common/SeguridadBase).
 *
 *  Público .................. GET /productos, /productos/{codigo}, /categorias, /categorias/{id}
 *                             (la tienda se ve sin cuenta; el Vendedor usa estas mismas lecturas)
 *  Solo Administrador ....... GET /productos/criticos y crear/editar/eliminar productos y categorías
 *  Servicio interno ......... /internal/** (clave X-Internal-Api-Key, la usa pedidos)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final String ADMIN = "ADMINISTRADOR";
	private static final String API = "/api/v1";

	@Bean
	SecurityFilterChain cadenaSeguridad(HttpSecurity http, JwtUtil jwt, ObjectMapper mapper,
			@Value("${app.internal.api-key}") String claveInterna) throws Exception {
		SeguridadBase.aplicar(http, jwt, claveInterna, mapper)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/h2-console/**", "/error").permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

						.requestMatchers(HttpMethod.GET, API + "/productos/criticos").hasRole(ADMIN)
						.requestMatchers(HttpMethod.GET, API + "/productos", API + "/productos/*").permitAll()
						.requestMatchers(HttpMethod.GET, API + "/categorias", API + "/categorias/*").permitAll()
						.requestMatchers(API + "/productos", API + "/productos/**").hasRole(ADMIN)
						.requestMatchers(API + "/categorias", API + "/categorias/**").hasRole(ADMIN)

						.requestMatchers("/internal/**").hasRole(ClaveInternaFiltro.ROL)

						.anyRequest().denyAll());
		return http.build();
	}
}
