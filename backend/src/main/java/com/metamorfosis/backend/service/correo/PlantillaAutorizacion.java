package com.metamorfosis.backend.service.correo;

import java.util.Locale;

/**
 * HU8: contenido del correo que pide autorizar el registro. HTML con estilos en línea y
 * tablas, que es lo que respetan Gmail, Outlook y los clientes de celular.
 * Internacionalización: se escribe en español o en inglés, según el idioma de quien pidió el registro.
 */
public final class PlantillaAutorizacion {

    public static final String ASUNTO = "Autoriza la creación de tu cuenta en Metamorfosis";

    /** Textos del correo en un idioma. */
    private record Textos(String lang, String asunto, String titulo, String pidio, String explicacion,
                          String autorizar, String noFuiYo, String vence) {}

    private static final Textos ES = new Textos("es", ASUNTO,
            "¿Quieres crear una cuenta en Metamorfosis?",
            "Alguien pidió registrarse con este correo:",
            "Si fuiste tú, autoriza y termina tu registro. Si no, avísanos y no se creará ninguna cuenta.",
            "Autorizar y continuar", "No fui yo",
            "El enlace vence en %d minutos y solo se puede usar una vez.");

    private static final Textos EN = new Textos("en", "Authorize the creation of your Metamorfosis account",
            "Do you want to create a Metamorfosis account?",
            "Someone asked to sign up with this email:",
            "If it was you, authorize it and finish signing up. If not, let us know and no account will be created.",
            "Authorize and continue", "It wasn't me",
            "The link expires in %d minutes and can only be used once.");

    private PlantillaAutorizacion() {}

    private static Textos textos(Locale idioma) {
        return idioma != null && "en".equals(idioma.getLanguage()) ? EN : ES;
    }

    public static String asunto(Locale idioma) {
        return textos(idioma).asunto();
    }

    public static String html(String email, String urlAutorizar, String urlRechazar, int minutosVigencia) {
        return html(email, urlAutorizar, urlRechazar, minutosVigencia, null);
    }

    public static String html(String email, String urlAutorizar, String urlRechazar, int minutosVigencia, Locale idioma) {
        Textos t = textos(idioma);
        return """
                <!doctype html>
                <html lang="%s">
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#f8fafc;font-family:Nunito,Segoe UI,Arial,sans-serif;color:#111827;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;padding:32px 16px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border-radius:16px;overflow:hidden;">
                        <tr><td style="background:#7f22fe;padding:24px 32px;">
                          <span style="font-size:22px;font-weight:700;color:#ffffff;">Metamorfosis</span>
                        </td></tr>
                        <tr><td style="padding:32px;">
                          <h1 style="margin:0 0 12px;font-size:22px;">%s</h1>
                          <p style="margin:0 0 8px;font-size:15px;line-height:24px;color:#4b5563;">
                            %s <strong style="color:#111827;">%s</strong>.
                          </p>
                          <p style="margin:0 0 24px;font-size:15px;line-height:24px;color:#4b5563;">
                            %s
                          </p>
                          <table role="presentation" cellpadding="0" cellspacing="0"><tr>
                            <td style="border-radius:12px;background:#7f22fe;">
                              <a href="%s" style="display:inline-block;padding:14px 24px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;">%s</a>
                            </td>
                            <td style="width:12px;"></td>
                            <td style="border-radius:12px;border:1px solid #e5e7eb;">
                              <a href="%s" style="display:inline-block;padding:13px 20px;font-size:15px;font-weight:700;color:#374151;text-decoration:none;">%s</a>
                            </td>
                          </tr></table>
                          <p style="margin:24px 0 0;font-size:13px;line-height:20px;color:#6b7280;">
                            %s
                          </p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(t.lang(), t.titulo(), t.pidio(), escapar(email), t.explicacion(),
                escapar(urlAutorizar), t.autorizar(), escapar(urlRechazar), t.noFuiYo(),
                t.vence().formatted(minutosVigencia));
    }

    public static String texto(String email, String urlAutorizar, String urlRechazar, int minutosVigencia) {
        return texto(email, urlAutorizar, urlRechazar, minutosVigencia, null);
    }

    public static String texto(String email, String urlAutorizar, String urlRechazar, int minutosVigencia, Locale idioma) {
        Textos t = textos(idioma);
        return """
                %s
                %s %s

                %s: %s
                %s: %s

                %s"""
                .formatted(t.titulo(), t.pidio(), email, t.autorizar(), urlAutorizar, t.noFuiYo(), urlRechazar,
                        t.vence().formatted(minutosVigencia));
    }

    /** Evita que un email con caracteres raros rompa el HTML del correo. */
    static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
