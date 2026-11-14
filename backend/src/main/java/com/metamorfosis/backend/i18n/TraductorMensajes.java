package com.metamorfosis.backend.i18n;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Internacionalización de los mensajes de la API. Los servicios escriben sus mensajes en español
 * (el idioma por defecto); aquí, justo antes de responder, se traducen al idioma que pidió el cliente
 * en la cabecera Accept-Language. Las traducciones están en {@code i18n/mensajes_<idioma>.txt}.
 * Un mensaje sin traducción se devuelve en español.
 */
@Component
public class TraductorMensajes {

    public static final Locale ESPANOL = Locale.forLanguageTag("es");
    public static final Locale INGLES = Locale.ENGLISH;
    public static final List<Locale> IDIOMAS = List.of(ESPANOL, INGLES);

    private static final String SEPARADOR = " => ";
    private static final Pattern MARCADOR = Pattern.compile("\\{(\\d+)}");

    /** Por idioma: traducciones exactas y plantillas con partes variables ({0}, {1}...). */
    private final Map<String, Map<String, String>> exactas = new HashMap<>();
    private final Map<String, List<Plantilla>> plantillas = new HashMap<>();

    private record Plantilla(Pattern patron, List<Integer> orden, String destino) {}

    public TraductorMensajes() {
        for (Locale idioma : IDIOMAS) {
            if (!idioma.equals(ESPANOL)) cargar(idioma.getLanguage());
        }
    }

    /** Traduce un mensaje en español al idioma pedido (o lo deja igual si no hay traducción). */
    public String traducir(String mensaje, Locale idioma) {
        if (mensaje == null || idioma == null) return mensaje;
        String lengua = idioma.getLanguage();
        Map<String, String> exactasIdioma = exactas.get(lengua);
        if (exactasIdioma == null) return mensaje;
        String exacta = exactasIdioma.get(mensaje);
        if (exacta != null) return exacta;
        for (Plantilla p : plantillas.getOrDefault(lengua, List.of())) {
            Matcher m = p.patron().matcher(mensaje);
            if (m.matches()) return completar(p, m);
        }
        return mensaje;
    }

    public List<String> traducir(List<String> mensajes, Locale idioma) {
        if (mensajes == null) return List.of();
        return mensajes.stream().map(m -> traducir(m, idioma)).toList();
    }

    private static String completar(Plantilla p, Matcher m) {
        String resultado = p.destino();
        for (int grupo = 1; grupo <= m.groupCount(); grupo++) {
            int indice = p.orden().get(grupo - 1);
            resultado = resultado.replace("{" + indice + "}", m.group(grupo));
        }
        return resultado;
    }

    private void cargar(String lengua) {
        String ruta = "/i18n/mensajes_" + lengua + ".txt";
        Map<String, String> exactasIdioma = new HashMap<>();
        List<Plantilla> plantillasIdioma = new ArrayList<>();
        try (InputStream entrada = TraductorMensajes.class.getResourceAsStream(ruta)) {
            if (entrada == null) throw new IllegalStateException("Falta el archivo de traducciones " + ruta);
            BufferedReader lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
            String linea;
            while ((linea = lector.readLine()) != null) {
                linea = linea.strip();
                int corte = linea.indexOf(SEPARADOR);
                if (linea.isEmpty() || linea.startsWith("#") || corte < 0) continue;
                String origen = linea.substring(0, corte).strip();
                String destino = linea.substring(corte + SEPARADOR.length()).strip();
                if (MARCADOR.matcher(origen).find()) plantillasIdioma.add(plantilla(origen, destino));
                else exactasIdioma.put(origen, destino);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        exactas.put(lengua, exactasIdioma);
        plantillas.put(lengua, plantillasIdioma);
    }

    /** "La meta ya tiene el máximo de {0} personas" -> patrón que captura el {0}. */
    private static Plantilla plantilla(String origen, String destino) {
        StringBuilder patron = new StringBuilder();
        List<Integer> orden = new ArrayList<>();
        Matcher m = MARCADOR.matcher(origen);
        int desde = 0;
        while (m.find()) {
            patron.append(Pattern.quote(origen.substring(desde, m.start()))).append("(.+?)");
            orden.add(Integer.parseInt(m.group(1)));
            desde = m.end();
        }
        patron.append(Pattern.quote(origen.substring(desde)));
        return new Plantilla(Pattern.compile(patron.toString(), Pattern.DOTALL), orden, destino);
    }
}
