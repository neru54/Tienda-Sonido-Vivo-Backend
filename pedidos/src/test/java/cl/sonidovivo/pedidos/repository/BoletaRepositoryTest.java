package cl.sonidovivo.pedidos.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import cl.sonidovivo.pedidos.model.Boleta;
import cl.sonidovivo.pedidos.model.DetalleBoleta;

/** Numeración desde 1001, filtros por estado y RUN, y unicidad de la Idempotency-Key, contra H2 real. */
@DataJpaTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // la secuencia no retrocede con el rollback: la numeración se prueba primero
class BoletaRepositoryTest {

	@Autowired
	BoletaRepository repo;

	static Boleta boleta(String run, String estado, String clave) {
		Boleta b = new Boleta();
		b.setFecha(Instant.now());
		b.setEstado(estado);
		b.setUsuarioRun(run);
		b.setNombreCliente("N");
		b.setApellidosCliente("A");
		b.setCorreoCliente("c@gmail.com");
		b.setCalle("Calle");
		b.setRegion("R");
		b.setComuna("C");
		b.setIdempotencyKey(clave);
		b.agregarDetalle(new DetalleBoleta("GA001", "Guitarra", "Guitarras Acústicas", 1, 1000));
		return b;
	}

	@Order(1)
	@Test
	void laNumeracionEmpiezaEn1001YSeFiltraPorEstadoYRun() {
		Boleta primera = repo.saveAndFlush(boleta("33333333-3", Boleta.ESTADO_PAGADA, null));
		repo.saveAndFlush(boleta("33333333-3", Boleta.ESTADO_PENDIENTE, null));
		repo.saveAndFlush(boleta(null, Boleta.ESTADO_PAGADA, null));

		assertThat(primera.getId()).isEqualTo(1001L);
		assertThat(repo.findByEstadoOrderByFechaDescIdDesc(Boleta.ESTADO_PAGADA)).hasSize(2);
		assertThat(repo.findByUsuarioRunAndEstadoOrderByFechaDescIdDesc("33333333-3", Boleta.ESTADO_PAGADA)).hasSize(1)
				.first().satisfies(b -> assertThat(b.getDetalles()).hasSize(1));
		assertThat(repo.findByIdAndEstado(primera.getId() + 1, Boleta.ESTADO_PAGADA)).isEmpty(); // la pendiente no se muestra
	}

	@Test
	void laIdempotencyKeyEsUnica() {
		repo.saveAndFlush(boleta(null, Boleta.ESTADO_PAGADA, "misma-clave"));
		assertThat(repo.findByIdempotencyKey("misma-clave")).isPresent();
		assertThatThrownBy(() -> repo.saveAndFlush(boleta(null, Boleta.ESTADO_PAGADA, "misma-clave")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}
}
