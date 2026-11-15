package com.metamorfosis.backend.i18n;

import com.metamorfosis.backend.service.correo.PlantillaContrasenaCambiada;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** HU32: el correo de aviso sale en el idioma de quien cambió la contraseña y no rompe el HTML. */
class PlantillaContrasenaCambiadaTest {

    private static final String URL = "https://metamorfosis.app/login";

    @Test
    void enEspanolPorDefecto() {
        String html = PlantillaContrasenaCambiada.html("Daniel", "daniel@correo.com", URL, null);

        assertEquals("Tu contraseña de Metamorfosis cambió", PlantillaContrasenaCambiada.asunto(null));
        assertTrue(html.contains("<html lang=\"es\">"));
        assertTrue(html.contains("Hola, Daniel:"));
        assertTrue(html.contains("daniel@correo.com"));
        assertTrue(html.contains(URL));
    }

    @Test
    void enIngles() {
        Locale ingles = Locale.forLanguageTag("en-US");
        String texto = PlantillaContrasenaCambiada.texto("Daniel", "daniel@correo.com", URL, ingles);

        assertEquals("Your Metamorfosis password changed", PlantillaContrasenaCambiada.asunto(ingles));
        assertTrue(texto.startsWith("Your password changed"));
        assertTrue(texto.contains("Go to Metamorfosis: " + URL));
        assertTrue(PlantillaContrasenaCambiada.html("Daniel", "d@c.com", URL, ingles).contains("<html lang=\"en\">"));
    }

    @Test
    void escapaElNombreEnElHtml() {
        String html = PlantillaContrasenaCambiada.html("<b>Ana</b>", "ana@correo.com", URL, null);

        assertFalse(html.contains("<b>Ana</b>"));
        assertTrue(html.contains("&lt;b&gt;Ana&lt;/b&gt;"));
    }
}
