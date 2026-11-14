package com.metamorfosis.backend.dto;

/** HU32: la contraseña actual, la nueva y la nueva repetida. */
public record CambiarContrasenaRequest(String actual, String nueva, String confirmacion) { }
