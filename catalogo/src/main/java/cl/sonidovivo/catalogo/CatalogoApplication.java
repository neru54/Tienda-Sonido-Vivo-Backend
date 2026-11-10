package cl.sonidovivo.catalogo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import cl.sonidovivo.common.config.ComunConfig;

/**
 * Microservicio CATALOGO (puerto 8082, esquema sonido_catalogo).
 * Dueño de categorías, productos, precios y stock. Es el ÚNICO que modifica el stock:
 * pedidos le pide descontarlo por la ruta interna /internal/ventas (con clave de servicio).
 * Solo VALIDA el JWT (no lo emite): lo firma usuarios con el secreto compartido.
 */
@SpringBootApplication
@Import(ComunConfig.class)
public class CatalogoApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatalogoApplication.class, args);
	}
}
