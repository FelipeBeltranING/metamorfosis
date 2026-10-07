package com.metamorfosis.backend.exception;

public class EmailyaRegistradoException extends RuntimeException {
    public EmailyaRegistradoException(String email) {
        super("El email " + email + " ya está registrado");
    }
}