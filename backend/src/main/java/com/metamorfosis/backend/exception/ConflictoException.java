package com.metamorfosis.backend.exception;

/** HU13: la acción choca con el estado actual (p. ej. la persona ya participa o ya está invitada) (409). */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
