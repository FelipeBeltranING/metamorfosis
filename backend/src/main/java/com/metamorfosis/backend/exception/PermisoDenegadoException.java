package com.metamorfosis.backend.exception;

public class PermisoDenegadoException extends RuntimeException {

    public PermisoDenegadoException(String mensaje) {
        super(mensaje);
    }
}