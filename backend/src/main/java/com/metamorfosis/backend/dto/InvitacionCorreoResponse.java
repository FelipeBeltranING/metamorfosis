package com.metamorfosis.backend.dto;

/**
 * HU14: lo que muestra la página del enlace del correo antes de responder. {@code paraEmail}
 * es el correo invitado enmascarado (l***@gmail.com), para saber con qué cuenta entrar.
 */
public record InvitacionCorreoResponse(Integer codigoMeta, String nombreMeta, String nombreInvitador,
                                       String paraEmail) { }
