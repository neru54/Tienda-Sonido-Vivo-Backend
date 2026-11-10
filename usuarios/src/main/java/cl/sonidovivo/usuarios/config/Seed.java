package cl.sonidovivo.usuarios.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cl.sonidovivo.common.security.TipoUsuario;
import cl.sonidovivo.usuarios.model.Usuario;
import cl.sonidovivo.usuarios.repository.UsuarioRepository;

/**
 * Carga los 3 usuarios de demostración (Administrador, Vendedor y Cliente) si la tabla está vacía.
 * Los datos están en resources/semilla/usuarios.json (los mismos del monolito) y las contraseñas
 * se guardan con BCrypt. Se desactiva con SEMILLA=false.
 */
@Component
@ConditionalOnProperty(name = "app.semilla.habilitada", havingValue = "true")
public class Seed implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(Seed.class);

	private final UsuarioRepository usuarios;
	private final PasswordEncoder encoder;
	private final ObjectMapper mapper;

	public Seed(UsuarioRepository usuarios, PasswordEncoder encoder, ObjectMapper mapper) {
		this.usuarios = usuarios;
		this.encoder = encoder;
		this.mapper = mapper;
	}

	@Override
	@Transactional
	public void run(String... args) throws IOException {
		if (usuarios.count() > 0) return;
		List<Map<String, String>> datos;
		try (InputStream in = new ClassPathResource("semilla/usuarios.json").getInputStream()) {
			datos = mapper.readValue(in, new TypeReference<List<Map<String, String>>>() {
			});
		}
		for (Map<String, String> d : datos) {
			Usuario u = new Usuario();
			u.setRun(d.get("run"));
			u.setNombre(d.get("nombre"));
			u.setApellidos(d.get("apellidos"));
			u.setCorreo(d.get("correo"));
			u.setPassword(encoder.encode(d.get("password")));
			String fecha = d.get("fechaNacimiento");
			u.setFechaNacimiento(fecha == null || fecha.isBlank() ? null : LocalDate.parse(fecha));
			u.setTipoUsuario(TipoUsuario.desde(d.get("tipoUsuario")));
			u.setRegion(d.get("region"));
			u.setComuna(d.get("comuna"));
			u.setDireccion(d.get("direccion"));
			usuarios.save(u);
		}
		log.info("Carga inicial: {} usuarios de demostración", datos.size());
	}
}
