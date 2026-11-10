package cl.sonidovivo.usuarios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.sonidovivo.common.security.JwtUtil;
import cl.sonidovivo.common.security.SeguridadBase;

/**
 * Reglas de acceso del microservicio usuarios (la parte común está en common/SeguridadBase).
 *
 *  Público .................. POST /auth/login y /auth/registro
 *  Cualquier autenticado .... GET /auth/me (lo necesita la sesión de los tres roles, incluido el Vendedor)
 *  Administrador y Cliente .. /usuarios/me (ver y editar el propio perfil). El Vendedor recibe 403:
 *                             según el enunciado solo ve productos y órdenes.
 *  Solo Administrador ....... CRUD /usuarios/**
 *
 * /usuarios/me/boletas y /usuarios/{run}/boletas NO están aquí: el Gateway las envía a pedidos.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final String ADMIN = "ADMINISTRADOR";
	private static final String CLIENTE = "CLIENTE";
	private static final String API = "/api/v1";

	@Bean
	SecurityFilterChain cadenaSeguridad(HttpSecurity http, JwtUtil jwt, ObjectMapper mapper) throws Exception {
		// usuarios no tiene endpoints /internal/**: no se configura clave interna ("" = ninguna aceptada)
		SeguridadBase.aplicar(http, jwt, "", mapper)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/h2-console/**", "/error").permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

						.requestMatchers(HttpMethod.POST, API + "/auth/login", API + "/auth/registro").permitAll()
						.requestMatchers(HttpMethod.GET, API + "/auth/me").authenticated()

						.requestMatchers(API + "/usuarios/me").hasAnyRole(ADMIN, CLIENTE)
						.requestMatchers(API + "/usuarios", API + "/usuarios/**").hasRole(ADMIN)

						.anyRequest().denyAll());
		return http.build();
	}

	/** BCrypt: cada hash incluye su propia sal, así dos contraseñas iguales no producen el mismo texto. */
	@Bean
	PasswordEncoder codificadorPassword() {
		return new BCryptPasswordEncoder();
	}
}
