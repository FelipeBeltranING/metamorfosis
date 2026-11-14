package com.metamorfosis.backend.model;

import java.util.Set;

/**
 * HU20: las piezas que se pueden elegir para el avatar. Son los valores del estilo «Big Smile» de
 * DiceBear (ilustraciones de Ashley Seo, licencia CC BY 4.0), que el frontend usa para dibujarlo.
 * La migración V26 tiene las mismas listas en sus restricciones check.
 */
public final class CatalogoAvatar {

    public static final String PIEL_INICIAL = "f5d7b1";
    public static final String PEINADO_INICIAL = "shortHair";
    public static final String COLOR_PELO_INICIAL = "71472d";
    public static final String OJOS_INICIALES = "cheery";
    public static final String BOCA_INICIAL = "openedSmile";

    public static final Set<String> PIELES = Set.of(
            "ffe4c0", "f5d7b1", "efcc9f", "e2ba87", "c99c62", "a47539", "8c5a2b", "643d19");

    public static final Set<String> PEINADOS = Set.of(
            "shortHair", "mohawk", "wavyBob", "bowlCutHair", "curlyBob", "straightHair", "braids",
            "shavedHead", "bunHair", "froBun", "bangs", "halfShavedHead", "curlyShortHair");

    public static final Set<String> COLORES_PELO = Set.of(
            "220f00", "3a1a00", "71472d", "e2ba87", "605de4", "238d80", "d56c0c", "e9b729");

    public static final Set<String> OJOS = Set.of(
            "cheery", "normal", "confused", "starstruck", "winking", "sleepy", "sad", "angry");

    public static final Set<String> BOCAS = Set.of(
            "openedSmile", "unimpressed", "gapSmile", "openSad", "teethSmile", "awkwardSmile", "braces", "kawaii");

    public static final Set<String> ACCESORIOS = Set.of(
            "catEars", "glasses", "sailormoonCrown", "clownNose", "sleepMask", "sunglasses", "faceMask", "mustache");

    private CatalogoAvatar() { }
}
