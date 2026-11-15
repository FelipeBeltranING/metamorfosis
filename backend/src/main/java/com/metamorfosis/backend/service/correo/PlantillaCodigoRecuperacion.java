package com.metamorfosis.backend.service.correo;

import java.util.Locale;

/**
 * HU33: correo con el código de 6 dígitos para cambiar la contraseña olvidada, en español o en
 * inglés según el idioma de quien lo pidió. El código se muestra en dos grupos de tres («482 913»)
 * para que se lea y se copie fácil.
 */
public final class PlantillaCodigoRecuperacion {

    /** Textos del correo en un idioma. */
    private record Textos(String lang, String asunto, String titulo, String hola, String explicacion,
                          String vence, String noFuiste) {}

    private static final Textos ES = new Textos("es", "Tu código para cambiar la contraseña de Metamorfosis",
            "Tu código para cambiar la contraseña",
            "Hola, %s:",
            "Pediste cambiar la contraseña de tu cuenta de Metamorfosis. Escribe este código en la página donde lo pediste:",
            "El código vence en %d minutos y solo sirve una vez.",
            "Si no lo pediste, ignora este correo: tu contraseña sigue igual.");

    private static final Textos EN = new Textos("en", "Your code to change your Metamorfosis password",
            "Your code to change your password",
            "Hi %s,",
            "You asked to change the password of your Metamorfosis account. Enter this code on the page where you asked for it:",
            "The code expires in %d minutes and can only be used once.",
            "If you didn't ask for it, ignore this email: your password stays the same.");

    private PlantillaCodigoRecuperacion() {}

    private static Textos textos(Locale idioma) {
        return idioma != null && "en".equals(idioma.getLanguage()) ? EN : ES;
    }

    /** «482913» → «482 913». */
    static String agrupar(String codigo) {
        return codigo.length() == 6 ? codigo.substring(0, 3) + " " + codigo.substring(3) : codigo;
    }

    public static String asunto(Locale idioma) {
        return textos(idioma).asunto();
    }

    public static String html(String nombre, String codigo, int minutosVigencia, Locale idioma) {
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
                          <p style="margin:0 0 20px;font-size:15px;line-height:24px;color:#4b5563;">%s</p>
                          <p style="margin:0 0 20px;padding:16px;border-radius:12px;background:#f5f3ff;text-align:center;font-size:34px;font-weight:800;letter-spacing:6px;color:#5b21b6;">%s</p>
                          <p style="margin:0 0 8px;font-size:14px;line-height:22px;color:#4b5563;">%s</p>
                          <p style="margin:0;font-size:13px;line-height:20px;color:#6b7280;">%s</p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(t.lang(), t.titulo(), t.hola().formatted(PlantillaAutorizacion.escapar(nombre)),
                t.explicacion(), agrupar(codigo), t.vence().formatted(minutosVigencia), t.noFuiste());
    }

    public static String texto(String nombre, String codigo, int minutosVigencia, Locale idioma) {
        Textos t = textos(idioma);
        return """
                %s

                %s
                %s

                %s

                %s
                %s"""
                .formatted(t.titulo(), t.hola().formatted(nombre), t.explicacion(), agrupar(codigo),
                        t.vence().formatted(minutosVigencia), t.noFuiste());
    }
}
