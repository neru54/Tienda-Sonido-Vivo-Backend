package cl.sonidovivo.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Metadatos de Swagger (OpenAPI) de cada microservicio. El título cambia por servicio
 * (app.openapi.titulo) y el Gateway junta los tres documentos en un selector.
 *
 * El esquema "bearerAuth" agrega el botón "Authorize" para pegar el JWT obtenido en
 * POST /api/v1/auth/login. El servidor "/" hace que Swagger llame al mismo host desde el que
 * se abrió: si se abre desde el Gateway (8080), las pruebas también pasan por el Gateway.
 */
@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI documentacion(@Value("${app.openapi.titulo:Sonido Vivo API}") String titulo,
			@Value("${app.openapi.descripcion:}") String descripcion) {
		return new OpenAPI()
				.info(new Info().title(titulo).version("v1").description(descripcion)
						.contact(new Contact().name("Equipo Sonido Vivo - DSY1104")))
				.addServersItem(new Server().url("/").description("Servidor actual (Gateway o servicio directo)"))
				.components(new Components().addSecuritySchemes("bearerAuth",
						new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
	}
}
