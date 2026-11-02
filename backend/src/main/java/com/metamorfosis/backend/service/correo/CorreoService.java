package com.metamorfosis.backend.service.correo;

/**
 * Envío de correos. Hay dos implementaciones: Brevo (API externa, producción) y consola
 * (desarrollo: no envía nada, escribe el correo en el log). Se elige con app.correo.proveedor.
 */
public interface CorreoService {

    /**
     * Envía un correo. Lanza CorreoNoEnviadoException si el proveedor lo rechaza.
     * @param textoPlano versión sin HTML, para clientes de correo que no muestran HTML
     */
    void enviar(String destino, String asunto, String html, String textoPlano);
}
