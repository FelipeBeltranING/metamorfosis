package com.metamorfosis.backend.config;

import com.metamorfosis.backend.service.correo.BrevoCorreoService;
import com.metamorfosis.backend.service.correo.ConsolaCorreoService;
import com.metamorfosis.backend.service.correo.CorreoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HU8: elige cómo se envían los correos según app.correo.proveedor:
 *   consola → no envía, escribe el correo en el log (desarrollo, valor por defecto)
 *   brevo   → API de Brevo; exige BREVO_API_KEY y un remitente verificado
 */
@Configuration
public class CorreoConfig {

    @Bean
    public CorreoService correoService(@Value("${app.correo.proveedor:consola}") String proveedor,
                                       @Value("${app.correo.brevo.api-key:}") String apiKey,
                                       @Value("${app.correo.remitente}") String remitente,
                                       @Value("${app.correo.remitente-nombre:Metamorfosis}") String remitenteNombre) {
        if ("brevo".equalsIgnoreCase(proveedor.trim())) {
            if (apiKey.isBlank()) {
                throw new IllegalStateException(
                        "app.correo.proveedor=brevo pero falta la clave: define la variable BREVO_API_KEY");
            }
            return new BrevoCorreoService(apiKey.trim(), remitente.trim(), remitenteNombre.trim());
        }
        return new ConsolaCorreoService();
    }
}
