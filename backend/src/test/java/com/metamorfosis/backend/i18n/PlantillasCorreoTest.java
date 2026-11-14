package com.metamorfosis.backend.i18n;

import com.metamorfosis.backend.service.correo.PlantillaAutorizacion;
import com.metamorfosis.backend.service.correo.PlantillaInvitacion;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Internacionalización: los correos salen en el idioma de quien hace la acción. */
class PlantillasCorreoTest {

    private static final String AUTORIZAR = "https://app/registro/autorizar?token=abc";
    private static final String RECHAZAR = "https://app/registro/rechazar?token=abc";

    @Test
    void elCorreoDeAutorizacionSaleEnEspanolPorDefecto() {
        String html = PlantillaAutorizacion.html("ana@correo.com", AUTORIZAR, RECHAZAR, 30);
        assertEquals(PlantillaAutorizacion.ASUNTO, PlantillaAutorizacion.asunto(null));
        assertTrue(html.contains("<html lang=\"es\">"));
        assertTrue(html.contains("Autorizar y continuar"));
        assertTrue(html.contains("El enlace vence en 30 minutos"));
    }

    @Test
    void elCorreoDeAutorizacionSaleEnIngles() {
        String html = PlantillaAutorizacion.html("ana@correo.com", AUTORIZAR, RECHAZAR, 30, Locale.ENGLISH);
        String texto = PlantillaAutorizacion.texto("ana@correo.com", AUTORIZAR, RECHAZAR, 30, Locale.ENGLISH);
        assertEquals("Authorize the creation of your Metamorfosis account", PlantillaAutorizacion.asunto(Locale.US));
        assertTrue(html.contains("<html lang=\"en\">"));
        assertTrue(html.contains("Authorize and continue"));
        assertTrue(html.contains("ana@correo.com"));
        assertTrue(texto.contains("The link expires in 30 minutes"));
        assertTrue(texto.contains(AUTORIZAR));
    }

    @Test
    void elCorreoDeInvitacionSaleEnIngles() {
        String html = PlantillaInvitacion.html("Laura Gómez", "Leer 20 libros", "https://a", "https://r", 7, Locale.ENGLISH);
        assertEquals("Laura Gómez invited you to a goal on Metamorfosis",
                PlantillaInvitacion.asunto("Laura Gómez", Locale.ENGLISH));
        assertTrue(html.contains("Laura Gómez invited you to a goal"));
        assertTrue(html.contains("Leer 20 libros"));
        assertTrue(html.contains("The link expires in 7 days"));
    }

    @Test
    void elCorreoDeInvitacionSaleEnEspanolSinIdioma() {
        assertEquals("Laura Gómez te invitó a una meta en Metamorfosis", PlantillaInvitacion.asunto("Laura Gómez"));
        String texto = PlantillaInvitacion.texto("Laura Gómez", "Leer 20 libros", "https://a", "https://r", 7);
        assertTrue(texto.startsWith("Laura Gómez te invitó a una meta en Metamorfosis: Leer 20 libros"));
        assertTrue(texto.contains("Aceptar invitación: https://a"));
    }
}
