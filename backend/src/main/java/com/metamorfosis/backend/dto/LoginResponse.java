package com.metamorfosis.backend.dto;

public record LoginResponse(
    String token,
    Integer codigoUsuario,
    String nombre,
    String apellido,
    String email
) { }