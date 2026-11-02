package com.metamorfosis.backend.dto;

/**
 * HU8: paso final del registro. El token sale del enlace del correo y de él se toma el
 * email, así que el email no viaja en el formulario y nadie puede cambiarlo.
 */
public record RegistroRequest(
    String token,
    String nombre,
    String apellido,
    String password
){}
