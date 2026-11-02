package com.metamorfosis.backend.service.correo;

import com.metamorfosis.backend.exception.CorreoNoEnviadoException;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * API externa de correo transaccional (Brevo): POST https://api.brevo.com/v3/smtp/email
 * con la cabecera api-key. El remitente debe estar verificado en la cuenta de Brevo.
 * Brevo acepta un solo tipo de cuerpo por envío, así que se manda el HTML.
 */
public class BrevoCorreoService implements CorreoService {

    static final String URL_API = "https://api.brevo.com/v3";
    private static final String MENSAJE_ERROR = "No pudimos enviar el correo. Intenta de nuevo en unos minutos.";

    private final RestClient cliente;
    private final String remitenteEmail;
    private final String remitenteNombre;

    public BrevoCorreoService(String apiKey, String remitenteEmail, String remitenteNombre) {
        this(RestClient.builder()
                .baseUrl(URL_API)
                .defaultHeader("api-key", apiKey)
                .build(), remitenteEmail, remitenteNombre);
    }

    BrevoCorreoService(RestClient cliente, String remitenteEmail, String remitenteNombre) {
        this.cliente = cliente;
        this.remitenteEmail = remitenteEmail;
        this.remitenteNombre = remitenteNombre;
    }

    @Override
    public void enviar(String destino, String asunto, String html, String textoPlano) {
        Map<String, Object> cuerpo = Map.of(
                "sender", Map.of("name", remitenteNombre, "email", remitenteEmail),
                "to", List.of(Map.of("email", destino)),
                "subject", asunto,
                "htmlContent", html);
        try {
            cliente.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new CorreoNoEnviadoException(MENSAJE_ERROR, e);
        }
    }
}
