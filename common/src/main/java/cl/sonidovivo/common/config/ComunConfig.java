package cl.sonidovivo.common.config;

import java.time.Clock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import cl.sonidovivo.common.security.JwtUtil;
import cl.sonidovivo.common.security.SesionActual;

/**
 * Beans compartidos. Cada microservicio la activa con @Import(ComunConfig.class) en su Application,
 * así queda explícito qué trae de common (sin escanear paquetes ajenos).
 *
 * Propiedades (definidas en el application.yml de cada servicio, con valor real por variable de entorno):
 *  app.jwt.secreto ............ JWT_SECRET (Base64, mínimo 32 bytes; el MISMO en los tres servicios)
 *  app.jwt.expiracion-minutos . JWT_EXPIRACION_MINUTOS
 */
@Configuration
@Import(OpenApiConfig.class)
public class ComunConfig {

	@Bean
	JwtUtil jwtUtil(@Value("${app.jwt.secreto}") String secreto,
			@Value("${app.jwt.expiracion-minutos:120}") long minutos) {
		return new JwtUtil(secreto, minutos);
	}

	@Bean
	SesionActual sesionActual() {
		return new SesionActual();
	}

	/** Reloj inyectable: las pruebas pueden fijar la hora. */
	@Bean
	Clock reloj() {
		return Clock.systemUTC();
	}
}
