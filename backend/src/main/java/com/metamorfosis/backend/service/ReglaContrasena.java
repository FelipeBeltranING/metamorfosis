package com.metamorfosis.backend.service;

import java.util.regex.Pattern;

/**
 * Regla de una contraseña nueva, la misma al registrarse (HU8) y al cambiarla (HU32): al menos
 * 8 caracteres, con una mayúscula, una minúscula y un número.
 */
public final class ReglaContrasena {

    public static final String MENSAJE =
            "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número";

    private static final Pattern PATRON = Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$");

    private ReglaContrasena() { }

    public static boolean cumple(String contrasena) {
        return contrasena != null && PATRON.matcher(contrasena).matches();
    }
}
