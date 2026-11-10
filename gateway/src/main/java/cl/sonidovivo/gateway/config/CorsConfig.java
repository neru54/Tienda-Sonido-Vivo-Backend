package cl.sonidovivo.gateway.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * CORS se aplica SOLO aquí: el navegador habla únicamente con el Gateway. Los microservicios tienen
 * CORS desactivado (no los llama el navegador), así no se duplican encabezados.
 *
 * CORS_ORIGINS: orígenes del frontend separados por coma (local: Vite 5173 y preview 4173;
 * en AWS se agrega la URL del sitio S3). Encabezados permitidos: Authorization (JWT),
 * Content-Type e Idempotency-Key (checkout). Sin credenciales: no se usan cookies.
 */
@Configuration
public class CorsConfig {

	@Bean
	CorsWebFilter filtroCors(@Value("${app.cors-origins}") String origenes) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOriginPatterns(Arrays.stream(origenes.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
		config.setAllowCredentials(false);
		config.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
		fuente.registerCorsConfiguration("/**", config);
		return new CorsWebFilter(fuente);
	}
}
