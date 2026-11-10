package cl.sonidovivo.common.util;

/** Expresiones regulares compartidas por las validaciones de los DTO (mismas reglas que el frontend). */
public final class Patrones {

	/** RUN sin puntos y con guion: 12345678-5 o 1234567-K. El dígito verificador se revisa en el servicio. */
	public static final String RUN = "^[0-9]{6,8}-[0-9kK]$";

	/** Solo se aceptan correos @duoc.cl, @profesor.duoc.cl o @gmail.com (regla del ERS). */
	public static final String CORREO_PERMITIDO = "(?i)^[^\\s@]+@(duoc\\.cl|profesor\\.duoc\\.cl|gmail\\.com)$";

	public static final String MSJ_CORREO = "Solo se aceptan correos @duoc.cl, @profesor.duoc.cl o @gmail.com.";
	public static final String MSJ_RUN = "El RUN ingresado no es válido. Usa el formato 12345678-5.";
	public static final String MSJ_PASSWORD = "La contraseña debe tener entre 4 y 10 caracteres.";

	private Patrones() {
	}
}
