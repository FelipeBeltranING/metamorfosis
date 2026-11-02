package com.metamorfosis.backend.dto;

/** HU8: paso 1 del registro, solo el email al que se envía la autorización. */
public record SolicitudRegistroRequest(String email) {}
