package com.metamorfosis.backend.i18n;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Internacionalización: los mensajes de la API en español se traducen al idioma de la petición. */
class TraductorMensajesTest {

    private final TraductorMensajes traductor = new TraductorMensajes();

    @Test
    void enEspanolElMensajeQuedaIgual() {
        assertEquals("Email o contraseña incorrectos",
                traductor.traducir("Email o contraseña incorrectos", TraductorMensajes.ESPANOL));
    }

    @Test
    void traduceUnMensajeFijoAlIngles() {
        assertEquals("Incorrect email or password",
                traductor.traducir("Email o contraseña incorrectos", Locale.ENGLISH));
    }

    @Test
    void conservaLasPartesVariablesDelMensaje() {
        assertEquals("Laura Gómez already has a pending invitation to this goal",
                traductor.traducir("Laura Gómez ya tiene una invitación pendiente a esta meta", Locale.ENGLISH));
        assertEquals("The goal already has the maximum of 10 people",
                traductor.traducir("La meta ya tiene el máximo de 10 personas", Locale.ENGLISH));
        assertEquals("Too many failed attempts. Try again in 15 minute(s)",
                traductor.traducir("Demasiados intentos fallidos. Intenta de nuevo en 15 minuto(s)", Locale.ENGLISH));
    }

    @Test
    void usaSoloElIdiomaSinImportarLaRegion() {
        assertEquals("Invalid data", traductor.traducir("Datos inválidos", Locale.US));
    }

    @Test
    void unMensajeSinTraduccionSeDevuelveEnEspanol() {
        assertEquals("Mensaje nuevo sin traducir", traductor.traducir("Mensaje nuevo sin traducir", Locale.ENGLISH));
    }

    @Test
    void unIdiomaNoSoportadoDejaElMensajeEnEspanol() {
        assertEquals("Datos inválidos", traductor.traducir("Datos inválidos", Locale.FRENCH));
    }

    @Test
    void traduceListasDeErrores() {
        assertEquals(List.of("Name is required", "The term is required"),
                traductor.traducir(List.of("El nombre es obligatorio", "El plazo es obligatorio"), Locale.ENGLISH));
    }

    @Test
    void sinMensajeNoFalla() {
        assertEquals(null, traductor.traducir((String) null, Locale.ENGLISH));
        assertEquals(List.of(), traductor.traducir((List<String>) null, Locale.ENGLISH));
    }
}
