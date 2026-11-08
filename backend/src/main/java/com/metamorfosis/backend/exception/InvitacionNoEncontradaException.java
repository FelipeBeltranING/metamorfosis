package com.metamorfosis.backend.exception;

/** HU14: el usuario no tiene una invitación pendiente a esa meta (404). */
public class InvitacionNoEncontradaException extends RuntimeException {

    public InvitacionNoEncontradaException() {
        super("No tienes una invitación pendiente a esta meta");
    }
}
