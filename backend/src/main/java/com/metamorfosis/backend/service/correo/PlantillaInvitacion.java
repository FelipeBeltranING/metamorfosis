package com.metamorfosis.backend.service.correo;

import java.util.Locale;

/**
 * HU13: correo que invita a una meta, con Aceptar y Rechazar. Mismo estilo que el de HU8:
 * HTML con estilos en línea y tablas, que es lo que respetan Gmail, Outlook y los celulares.
 * Internacionalización: se escribe en español o en inglés, según el idioma de quien invita.
 */
public final class PlantillaInvitacion {

    /** Textos del correo en un idioma. Los %s / %d se completan con el nombre, la meta y los días. */
    private record Textos(String lang, String asunto, String titulo, String quiere, String siAceptas,
                          String aceptar, String rechazar, String vence, String textoPlano) {}

    private static final Textos ES = new Textos("es",
            "%s te invitó a una meta en Metamorfosis",
            "%s te invitó a una meta",
            "Quiere que recorran juntos esta meta:",
            "Si aceptas, la meta aparecerá en tus metas: compartirán el camino y cada uno marcará sus propios días.",
            "Aceptar invitación", "Rechazar",
            "El enlace vence en %d días. También puedes responder desde Metamorfosis, en tus invitaciones.",
            "%s te invitó a una meta en Metamorfosis: %s");

    private static final Textos EN = new Textos("en",
            "%s invited you to a goal on Metamorfosis",
            "%s invited you to a goal",
            "They want to walk this goal together with you:",
            "If you accept, the goal will appear in your goals: you'll share the path and each person will check off their own days.",
            "Accept invitation", "Decline",
            "The link expires in %d days. You can also answer from Metamorfosis, in your invitations.",
            "%s invited you to a goal on Metamorfosis: %s");

    private PlantillaInvitacion() {}

    private static Textos textos(Locale idioma) {
        return idioma != null && "en".equals(idioma.getLanguage()) ? EN : ES;
    }

    public static String asunto(String nombreInvitador) {
        return asunto(nombreInvitador, null);
    }

    public static String asunto(String nombreInvitador, Locale idioma) {
        return textos(idioma).asunto().formatted(nombreInvitador);
    }

    public static String html(String nombreInvitador, String nombreMeta, String urlAceptar, String urlRechazar,
                              int diasVigencia) {
        return html(nombreInvitador, nombreMeta, urlAceptar, urlRechazar, diasVigencia, null);
    }

    public static String html(String nombreInvitador, String nombreMeta, String urlAceptar, String urlRechazar,
                              int diasVigencia, Locale idioma) {
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
                          <p style="margin:0 0 20px;font-size:15px;line-height:24px;color:#4b5563;">
                            %s
                          </p>
                          <p style="margin:0 0 24px;padding:16px 20px;border-radius:12px;background:#f5f3ff;font-size:18px;font-weight:700;color:#5b21b6;">
                            %s
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
                """.formatted(t.lang(), t.titulo().formatted(PlantillaAutorizacion.escapar(nombreInvitador)), t.quiere(),
                PlantillaAutorizacion.escapar(nombreMeta), t.siAceptas(),
                PlantillaAutorizacion.escapar(urlAceptar), t.aceptar(),
                PlantillaAutorizacion.escapar(urlRechazar), t.rechazar(),
                t.vence().formatted(diasVigencia));
    }

    public static String texto(String nombreInvitador, String nombreMeta, String urlAceptar, String urlRechazar,
                               int diasVigencia) {
        return texto(nombreInvitador, nombreMeta, urlAceptar, urlRechazar, diasVigencia, null);
    }

    public static String texto(String nombreInvitador, String nombreMeta, String urlAceptar, String urlRechazar,
                               int diasVigencia, Locale idioma) {
        Textos t = textos(idioma);
        return """
                %s

                %s: %s
                %s: %s

                %s"""
                .formatted(t.textoPlano().formatted(nombreInvitador, nombreMeta), t.aceptar(), urlAceptar,
                        t.rechazar(), urlRechazar, t.vence().formatted(diasVigencia));
    }
}
