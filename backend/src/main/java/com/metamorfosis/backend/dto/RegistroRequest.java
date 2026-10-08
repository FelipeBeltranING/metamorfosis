package com.metamorfosis.backend.dto;

public record RegistroRequest(
    String nombre,
    String apellido,
    String email,
    String password){

}