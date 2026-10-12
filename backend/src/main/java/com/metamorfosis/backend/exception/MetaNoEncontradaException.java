package com.metamorfosis.backend.exception;

public class MetaNoEncontradaException extends RuntimeException {

    public MetaNoEncontradaException(Integer codigoMeta) {
        super("No existe la meta con código " + codigoMeta);
    }
}