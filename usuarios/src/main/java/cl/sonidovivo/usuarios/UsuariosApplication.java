package cl.sonidovivo.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import cl.sonidovivo.common.config.ComunConfig;

/**
 * Microservicio USUARIOS (puerto 8081, esquema sonido_usuarios).
 * Responsable de las cuentas y del inicio de sesión: es el ÚNICO que conoce contraseñas (BCrypt)
 * y el único que EMITE el JWT. catalogo y pedidos solo lo validan con el secreto compartido.
 *
 * Esta clase queda en la raíz del paquete para que Spring encuentre controller, service, etc.
 * @Import(ComunConfig) trae de common el JwtUtil, SesionActual, el reloj y Swagger.
 */
@SpringBootApplication
@Import(ComunConfig.class)
public class UsuariosApplication {

	public static void main(String[] args) {
		SpringApplication.run(UsuariosApplication.class, args);
	}
}
