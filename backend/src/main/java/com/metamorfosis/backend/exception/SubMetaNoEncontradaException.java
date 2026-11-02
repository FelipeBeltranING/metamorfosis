package com.metamorfosis.backend.exception;

/** La sub-meta pedida no existe (404). */
public class SubMetaNoEncontradaException extends RuntimeException {

    public SubMetaNoEncontradaException(Integer codigoSubMeta) {
        super("No se encontró la sub-meta " + codigoSubMeta);
    }
}
