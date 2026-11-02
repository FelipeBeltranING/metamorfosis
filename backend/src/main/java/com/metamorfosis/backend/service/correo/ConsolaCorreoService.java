package com.metamorfosis.backend.service.correo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Desarrollo: no envía correos, los escribe en el log del backend para poder copiar el enlace.
 * Se usa cuando app.correo.proveedor=consola (valor por defecto).
 */
public class ConsolaCorreoService implements CorreoService {

    private static final Logger log = LoggerFactory.getLogger(ConsolaCorreoService.class);

    @Override
    public void enviar(String destino, String asunto, String html, String textoPlano) {
        log.info("""

                ===== CORREO (modo consola, no se envió) =====
                Para:   {}
                Asunto: {}

                {}
                ==============================================""", destino, asunto, textoPlano);
    }
}
