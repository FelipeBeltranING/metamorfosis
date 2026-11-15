package com.metamorfosis.backend.i18n;

import com.metamorfosis.backend.service.correo.PlantillaCodigoRecuperacion;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/** HU33: el correo con el código sale en el idioma de quien lo pidió y muestra el código agrupado. */
class PlantillaCodigoRecuperacionTest {

    @Test
    void enEspanolConElCodigoEnDosGrupos() {
        String html = PlantillaCodigoRecuperacion.html("Daniel", "482913", 15, null);

        assertEquals("Tu código para cambiar la contraseña de Metamorfosis", PlantillaCodigoRecuperacion.asunto(null));
        assertTrue(html.contains("<html lang=\"es\">"));
        assertTrue(html.contains("Hola, Daniel:"));
        assertTrue(html.contains("482 913"));
        assertTrue(html.contains("vence en 15 minutos"));
    }

    @Test
    void enIngles() {
        Locale ingles = Locale.forLanguageTag("en");
        String texto = PlantillaCodigoRecuperacion.texto("Daniel", "007001", 15, ingles);

        assertEquals("Your code to change your Metamorfosis password", PlantillaCodigoRecuperacion.asunto(ingles));
        assertTrue(texto.contains("007 001"));
        assertTrue(texto.contains("expires in 15 minutes"));
    }

    @Test
    void escapaElNombreEnElHtml() {
        String html = PlantillaCodigoRecuperacion.html("<i>Ana</i>", "123456", 15, null);

        assertFalse(html.contains("<i>Ana</i>"));
    }
}
