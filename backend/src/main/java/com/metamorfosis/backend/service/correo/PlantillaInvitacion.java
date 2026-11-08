package com.metamorfosis.backend.service.correo;

/**
 * HU13: correo que invita a una meta, con Aceptar y Rechazar. Mismo estilo que el de HU8:
 * HTML con estilos en línea y tablas, que es lo que respetan Gmail, Outlook y los celulares.
 */
public final class PlantillaInvitacion {

    private PlantillaInvitacion() {}

    public static String asunto(String nombreInvitador) {
        return nombreInvitador + " te invitó a una meta en Metamorfosis";
    }

    public static String html(String nombreInvitador, String nombreMeta, String urlAceptar, String urlRechazar,
                              int diasVigencia) {
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
                          <h1 style="margin:0 0 12px;font-size:22px;">%s te invitó a una meta</h1>
                          <p style="margin:0 0 20px;font-size:15px;line-height:24px;color:#4b5563;">
                            Quiere que recorran juntos esta meta:
                          </p>
                          <p style="margin:0 0 24px;padding:16px 20px;border-radius:12px;background:#f5f3ff;font-size:18px;font-weight:700;color:#5b21b6;">
                            %s
                          </p>
                          <p style="margin:0 0 24px;font-size:15px;line-height:24px;color:#4b5563;">
                            Si aceptas, la meta aparecerá en tus metas: compartirán el camino y cada uno marcará sus propios días.
                          </p>
                          <table role="presentation" cellpadding="0" cellspacing="0"><tr>
                            <td style="border-radius:12px;background:#7f22fe;">
                              <a href="%s" style="display:inline-block;padding:14px 24px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;">Aceptar invitación</a>
                            </td>
                            <td style="width:12px;"></td>
                            <td style="border-radius:12px;border:1px solid #e5e7eb;">
                              <a href="%s" style="display:inline-block;padding:13px 20px;font-size:15px;font-weight:700;color:#374151;text-decoration:none;">Rechazar</a>
                            </td>
                          </tr></table>
                          <p style="margin:24px 0 0;font-size:13px;line-height:20px;color:#6b7280;">
                            El enlace vence en %d días. También puedes responder desde Metamorfosis, en tus invitaciones.
                          </p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(PlantillaAutorizacion.escapar(nombreInvitador), PlantillaAutorizacion.escapar(nombreMeta),
                PlantillaAutorizacion.escapar(urlAceptar), PlantillaAutorizacion.escapar(urlRechazar), diasVigencia);
    }

    public static String texto(String nombreInvitador, String nombreMeta, String urlAceptar, String urlRechazar,
                               int diasVigencia) {
        return """
                %s te invitó a una meta en Metamorfosis: %s

                Aceptar invitación: %s
                Rechazar:           %s

                El enlace vence en %d días. También puedes responder desde Metamorfosis, en tus invitaciones."""
                .formatted(nombreInvitador, nombreMeta, urlAceptar, urlRechazar, diasVigencia);
    }
}
