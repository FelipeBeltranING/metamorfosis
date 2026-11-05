package com.metamorfosis.backend.dto;

/** HU-23: nombre y apellido nuevos. El correo no se edita: es con el que se inicia sesión. */
public record EditarCuentaRequest(String nombre, String apellido) { }
