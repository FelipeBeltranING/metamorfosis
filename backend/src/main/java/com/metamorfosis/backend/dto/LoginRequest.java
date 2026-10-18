package com.metamorfosis.backend.dto;

public record LoginRequest(
    String email,
    String password
){}