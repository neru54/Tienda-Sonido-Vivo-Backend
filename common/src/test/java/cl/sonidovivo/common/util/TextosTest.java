package cl.sonidovivo.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Reglas puras de texto: RUN chileno, slug de categoría y limpieza de campos opcionales. */
class TextosTest {

	@ParameterizedTest
	@CsvSource({ "12345678-5,true", "11111111-1,true", "12.345.678-5,false", "12345678-9,false", "1234567-k,false", "11111111,false" })
	void validaElRun(String run, boolean esperado) {
		assertThat(Textos.runValido(run)).isEqualTo(esperado);
	}

	@Test
	void runConDigitoK() {
		assertThat(Textos.runValido("10000013-k")).isTrue();
		assertThat(Textos.runValido(null)).isFalse();
		assertThat(Textos.normalizarRun(" 10000013-k ")).isEqualTo("10000013-K");
	}

	@Test
	void slugQuitaTildesYEspacios() {
		assertThat(Textos.slug("Guitarras Acústicas")).isEqualTo("guitarras-acusticas");
		assertThat(Textos.slug("  Teclados y Pianos! ")).isEqualTo("teclados-y-pianos");
		assertThat(Textos.slug(null)).isEmpty();
	}

	@Test
	void limpiezaDeTextos() {
		assertThat(Textos.limpio("  ")).isNull();
		assertThat(Textos.limpio(" a ")).isEqualTo("a");
		assertThat(Textos.recortado(null)).isEmpty();
		assertThat(Textos.normalizarCodigo(" ga001 ")).isEqualTo("GA001");
	}

	@Test
	void correosPermitidos() {
		assertThat("camila@gmail.com".matches(Patrones.CORREO_PERMITIDO)).isTrue();
		assertThat("Prof@PROFESOR.duoc.cl".matches(Patrones.CORREO_PERMITIDO)).isTrue();
		assertThat("alguien@hotmail.com".matches(Patrones.CORREO_PERMITIDO)).isFalse();
	}
}
