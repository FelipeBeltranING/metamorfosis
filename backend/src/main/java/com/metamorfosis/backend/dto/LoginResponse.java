package com.metamorfosis.backend.dto;

public record LoginResponse (
    String token,
    Integer idUsuario)
    
{}
