package com.metamorfosis.backend.service.correo;

/**
 * HU8: contenido del correo que pide autorizar el registro. HTML con estilos en línea y
 * tablas, que es lo que respetan Gmail, Outlook y los clientes de celular.
 */
public final class PlantillaAutorizacion {

    public static final String ASUNTO = "Autoriza la creación de tu cuenta en Metamorfosis";

    private PlantillaAutorizacion() {}

    public static String html(String email, String urlAutorizar, String urlRechazar, int minutosVigencia) {
        return """
                <!doctype html>
                <html lang="es">
                <head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"></head>
                <body style="margin:0;padding:0;background:#f8fafc;font-family:Nunito,Segoe UI,Arial,sans-serif;color:#111827;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;padding:32px 16px;">
                    <tr><td align="center">
                      <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:520px;background:#ffffff;border-radius:16px;overflow:hidden;">
                        <tr><td style="background:#7f22fe;padding:24px 32px;">
                          <span style="font-size:22px;font-weight:700;color:#ffffff;">Metamorfosis</span>
                        </td></tr>
                        <tr><td style="padding:32px;">
                          <h1 style="margin:0 0 12px;font-size:22px;">¿Quieres crear una cuenta en Metamorfosis?</h1>
                          <p style="margin:0 0 8px;font-size:15px;line-height:24px;color:#4b5563;">
                            Alguien pidió registrarse con este correo: <strong style="color:#111827;">%s</strong>.
                          </p>
                          <p style="margin:0 0 24px;font-size:15px;line-height:24px;color:#4b5563;">
                            Si fuiste tú, autoriza y termina tu registro. Si no, avísanos y no se creará ninguna cuenta.
                          </p>
                          <table role="presentation" cellpadding="0" cellspacing="0"><tr>
                            <td style="border-radius:12px;background:#7f22fe;">
                              <a href="%s" style="display:inline-block;padding:14px 24px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;">Autorizar y continuar</a>
                            </td>
                            <td style="width:12px;"></td>
                            <td style="border-radius:12px;border:1px solid #e5e7eb;">
                              <a href="%s" style="display:inline-block;padding:13px 20px;font-size:15px;font-weight:700;color:#374151;text-decoration:none;">No fui yo</a>
                            </td>
                          </tr></table>
                          <p style="margin:24px 0 0;font-size:13px;line-height:20px;color:#6b7280;">
                            El enlace vence en %d minutos y solo se puede usar una vez.
                          </p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(escapar(email), escapar(urlAutorizar), escapar(urlRechazar), minutosVigencia);
    }

    public static String texto(String email, String urlAutorizar, String urlRechazar, int minutosVigencia) {
        return """
                ¿Quieres crear una cuenta en Metamorfosis?
                Alguien pidió registrarse con este correo: %s

                Autorizar y continuar: %s
                No fui yo:             %s

                El enlace vence en %d minutos y solo se puede usar una vez."""
                .formatted(email, urlAutorizar, urlRechazar, minutosVigencia);
    }

    /** Evita que un email con caracteres raros rompa el HTML del correo. */
    static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
