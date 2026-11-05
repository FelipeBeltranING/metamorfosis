package com.metamorfosis.backend.dto;

/**
 * HU-23: foto de perfil ya recortada por el navegador, en data URL
 * ("data:image/jpeg;base64,...", también png o webp).
 */
public record FotoPerfilRequest(String foto) { }
