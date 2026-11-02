package com.metamorfosis.backend.dto;

import java.time.LocalDateTime;

/** HU8: email de la solicitud y hasta cuándo sirve el enlace (hora UTC). */
public record SolicitudRegistroResponse(String email, LocalDateTime fechaExpiracion) {}
