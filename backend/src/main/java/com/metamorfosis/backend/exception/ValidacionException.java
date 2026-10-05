package com.metamorfosis.backend.exception;

import java.util.List;

public class ValidacionException extends RuntimeException {
    private final List<String> errores;

    public ValidacionException(List<String> errores) {
        super("Datos inválidos");
        this.errores = errores;
    }

    public List<String> getErrores() { return errores; }
}