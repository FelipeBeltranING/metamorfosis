package com.metamorfosis.backend.dto;

/**
 * HU32: la contraseña nueva y la nueva repetida. Dentro de la app no se pide la actual: ya hay una
 * sesión iniciada (quien la olvidó y no tiene sesión usa el código por correo, HU33).
 */
public record CambiarContrasenaRequest(String nueva, String confirmacion) { }
