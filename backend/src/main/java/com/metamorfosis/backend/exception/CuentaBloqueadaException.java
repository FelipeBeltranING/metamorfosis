package com.metamorfosis.backend.exception;

/** HU9: demasiados intentos fallidos; el login queda bloqueado unos minutos (HTTP 429). */
public class CuentaBloqueadaException extends RuntimeException {

    private final long segundosRestantes;

    public CuentaBloqueadaException(long segundosRestantes) {
        super("Demasiados intentos fallidos. Intenta de nuevo en "
                + Math.max(1, (segundosRestantes + 59) / 60) + " minuto(s)");
        this.segundosRestantes = segundosRestantes;
    }

    public long getSegundosRestantes() { return segundosRestantes; }
}
