package com.metamorfosis.backend.service.correo;

import java.util.Locale;

/**
 * HU32: aviso de que la contraseña de la cuenta cambió, en español o en inglés según el idioma de
 * quien la cambió. Si no fue la persona dueña de la cuenta, así se entera.
 */
public final class PlantillaContrasenaCambiada {

    /** Textos del correo en un idioma. */
    private record Textos(String lang, String asunto, String titulo, String hola, String cambio,
                          String sesiones, String noFuiste, String boton) {}

    private static final Textos ES = new Textos("es", "Tu contraseña de Metamorfosis cambió",
            "Tu contraseña cambió",
            "Hola, %s:",
            "Se cambió la contraseña de tu cuenta de Metamorfosis (%s).",
            "Por seguridad cerramos tu sesión en los demás dispositivos: allí tendrás que entrar con la contraseña nueva.",
            "Si no fuiste tú, alguien conoce tu contraseña. Avisa al equipo de Metamorfosis cuanto antes.",
            "Ir a Metamorfosis");

    private static final Textos EN = new Textos("en", "Your Metamorfosis password changed",
            "Your password changed",
            "Hi %s,",
            "The password of your Metamorfosis account (%s) was changed.",
            "For your security we signed you out on your other devices: you'll need the new password there.",
            "If it wasn't you, someone knows your password. Let the Metamorfosis team know as soon as possible.",
            "Go to Metamorfosis");

    private PlantillaContrasenaCambiada() {}

    private static Textos textos(Locale idioma) {
        return idioma != null && "en".equals(idioma.getLanguage()) ? EN : ES;
    }

    public static String asunto(Locale idioma) {
        return textos(idioma).asunto();
    }

    public static String html(String nombre, String email, String urlEntrar, Locale idioma) {
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
                          <p style="margin:0 0 8px;font-size:15px;line-height:24px;color:#4b5563;">%s</p>
                          <p style="margin:0 0 8px;font-size:15px;line-height:24px;color:#4b5563;">%s</p>
                          <p style="margin:0 0 16px;font-size:15px;line-height:24px;color:#4b5563;">%s</p>
                          <p style="margin:0 0 24px;padding:12px 16px;border-radius:12px;background:#fef3c7;font-size:14px;line-height:22px;color:#92400e;">%s</p>
                          <table role="presentation" cellpadding="0" cellspacing="0"><tr>
                            <td style="border-radius:12px;background:#7f22fe;">
                              <a href="%s" style="display:inline-block;padding:14px 24px;font-size:15px;font-weight:700;color:#ffffff;text-decoration:none;">%s</a>
                            </td>
                          </tr></table>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(t.lang(), t.titulo(), t.hola().formatted(PlantillaAutorizacion.escapar(nombre)),
                t.cambio().formatted(PlantillaAutorizacion.escapar(email)), t.sesiones(), t.noFuiste(),
                PlantillaAutorizacion.escapar(urlEntrar), t.boton());
    }

    public static String texto(String nombre, String email, String urlEntrar, Locale idioma) {
        Textos t = textos(idioma);
        return """
                %s

                %s
                %s
                %s

                %s

                %s: %s"""
                .formatted(t.titulo(), t.hola().formatted(nombre), t.cambio().formatted(email), t.sesiones(),
                        t.noFuiste(), t.boton(), urlEntrar);
    }
}
