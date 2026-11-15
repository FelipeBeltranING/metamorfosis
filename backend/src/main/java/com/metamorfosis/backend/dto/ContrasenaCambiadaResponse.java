package com.metamorfosis.backend.dto;

/**
 * HU32: token nuevo para la sesión desde la que se cambió la contraseña. Los tokens anteriores
 * (las demás sesiones y el de esta misma) dejan de servir.
 */
public record ContrasenaCambiadaResponse(String token) { }
