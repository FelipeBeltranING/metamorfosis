package com.metamorfosis.backend.controller;

public record RegistroRequest(
    String nombre,
    String apellido,
    String email,
    String password){

}