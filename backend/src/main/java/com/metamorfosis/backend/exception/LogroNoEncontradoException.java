package com.metamorfosis.backend.exception;

/** HU21: el enlace no corresponde a ningún logro compartido (404). */
public class LogroNoEncontradoException extends RuntimeException {
    public LogroNoEncontradoException(String message) { super(message); }
}
