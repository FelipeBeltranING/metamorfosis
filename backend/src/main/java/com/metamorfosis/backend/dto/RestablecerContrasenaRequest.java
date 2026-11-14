package com.metamorfosis.backend.dto;

/** HU33: el correo, el código de 6 dígitos que llegó a ese correo y la contraseña nueva dos veces. */
public record RestablecerContrasenaRequest(String email, String codigo, String nueva, String confirmacion) { }
