package com.metamorfosis.backend.exception;

/** HU8: no se puede pedir otro correo todavía (reenvío muy seguido o el dueño respondió "No fui yo") (429). */
public class SolicitudBloqueadaException extends RuntimeException {

    public SolicitudBloqueadaException(String mensaje) {
        super(mensaje);
    }
}
