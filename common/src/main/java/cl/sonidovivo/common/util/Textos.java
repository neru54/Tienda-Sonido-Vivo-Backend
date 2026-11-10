package cl.sonidovivo.common.util;

import java.text.Normalizer;
import java.util.Locale;

/** Funciones de texto puras usadas por los servicios (iguales a las del monolito). */
public final class Textos {

	private Textos() {
	}

	/** "Guitarras Acústicas" -> "guitarras-acusticas" (id de categoría). */
	public static String slug(String texto) {
		String sinTildes = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
	}

	/** Valida un RUN chileno "12345678-5": formato y dígito verificador (módulo 11). */
	public static boolean runValido(String valor) {
		if (valor == null) return false;
		String run = valor.trim().toUpperCase(Locale.ROOT);
		if (!run.matches("^[0-9]{6,8}-[0-9K]$")) return false;
		String cuerpo = run.substring(0, run.length() - 2);
		char dv = run.charAt(run.length() - 1);
		int suma = 0;
		int multiplicador = 2;
		for (int i = cuerpo.length() - 1; i >= 0; i--) {
			suma += Character.getNumericValue(cuerpo.charAt(i)) * multiplicador;
			multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
		}
		int resto = 11 - (suma % 11);
		char esperado = resto == 11 ? '0' : resto == 10 ? 'K' : Character.forDigit(resto, 10);
		return dv == esperado;
	}

	/** RUN en mayúsculas y sin espacios (la K se guarda siempre mayúscula). */
	public static String normalizarRun(String run) {
		return run == null ? "" : run.trim().toUpperCase(Locale.ROOT);
	}

	/** Código de producto en mayúsculas y sin espacios. */
	public static String normalizarCodigo(String codigo) {
		return codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
	}

	/** Recorta y convierte "" en null (para columnas opcionales). */
	public static String limpio(String texto) {
		if (texto == null) return null;
		String t = texto.trim();
		return t.isEmpty() ? null : t;
	}

	/** Recorta y deja "" si viene null. */
	public static String recortado(String texto) {
		return texto == null ? "" : texto.trim();
	}
}
