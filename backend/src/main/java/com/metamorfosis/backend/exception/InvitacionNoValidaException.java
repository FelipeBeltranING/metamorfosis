package com.metamorfosis.backend.exception;

/** HU14: el enlace del correo no existe, ya se usó o venció (410). */
public class InvitacionNoValidaException extends RuntimeException {

    public InvitacionNoValidaException() {
        super("Este enlace ya no sirve: la invitación se respondió, se canceló o venció. "
                + "Si sigue pendiente, respóndela desde tus invitaciones en Metamorfosis.");
    }
}
