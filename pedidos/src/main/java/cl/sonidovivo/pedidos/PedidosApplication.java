package cl.sonidovivo.pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import cl.sonidovivo.common.config.ComunConfig;

/**
 * Microservicio PEDIDOS (puerto 8083, esquema sonido_pedidos).
 * Dueño de las boletas (checkout), del historial de compras y de los reportes de ventas.
 * No guarda productos ni usuarios: referencia al comprador por su RUN (del JWT) y a los productos por
 * su código, y le pide a catalogo por HTTP interno los precios reales y el descuento de stock.
 */
@SpringBootApplication
@Import(ComunConfig.class)
public class PedidosApplication {

	public static void main(String[] args) {
		SpringApplication.run(PedidosApplication.class, args);
	}
}
