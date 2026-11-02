package com.metamorfosis.backend.dto;

import java.time.LocalDateTime;

/**
 * HU8: email de la solicitud y hasta cuándo sirve el enlace (hora UTC).
 * `sesion` solo llega al pedir el registro: el navegador la guarda para saber cuándo se autorizó.
 */
public record SolicitudRegistroResponse(String email, LocalDateTime fechaExpiracion, String sesion) {}
