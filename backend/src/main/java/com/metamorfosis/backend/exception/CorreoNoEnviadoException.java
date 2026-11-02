package com.metamorfosis.backend.exception;

/** HU8: el servicio de correo no aceptó el envío (503). */
public class CorreoNoEnviadoException extends RuntimeException {

    public CorreoNoEnviadoException(String mensaje) {
        super(mensaje);
    }

    public CorreoNoEnviadoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
