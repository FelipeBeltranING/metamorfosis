package com.metamorfosis.backend.exception;

/** HU8: el enlace de autorización no existe, ya se usó o venció (410). */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
