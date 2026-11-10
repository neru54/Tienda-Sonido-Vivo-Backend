package cl.sonidovivo.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API GATEWAY (puerto 8080). El frontend solo conoce esta dirección: http://<host>:8080/api/v1.
 * No valida el JWT ni guarda datos; reenvía cada petición (con su Authorization) al microservicio
 * dueño del recurso, y es cada servicio el que decide 401/403 con sus propias reglas.
 */
@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}
}
