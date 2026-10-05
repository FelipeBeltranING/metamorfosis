package com.metamorfosis.backend.exception;

public class UsuarioNoEncontradoException extends RuntimeException {
    public UsuarioNoEncontradoException(Integer codigo) {
        super("No existe el usuario con código " + codigo);
    }
}