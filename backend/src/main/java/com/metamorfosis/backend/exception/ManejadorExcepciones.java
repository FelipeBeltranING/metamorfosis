package com.metamorfosis.backend.exception;

import com.metamorfosis.backend.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/** Traduce excepciones a códigos HTTP (400, 404, 500) sin filtrar detalles internos. */
@RestControllerAdvice
public class ManejadorExcepciones {

    private static final Logger log = LoggerFactory.getLogger(ManejadorExcepciones.class);

    @ExceptionHandler(ValidacionException.class)
    public ResponseEntity<ErrorResponse> validacion(ValidacionException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage(), e.getErrores());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> solicitudMalFormada(Exception e) {
        return responder(HttpStatus.BAD_REQUEST, "Solicitud mal formada", List.of());
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(UsuarioNoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> errorInterno(Exception e) {
        log.error("Error inesperado", e); // el detalle queda solo en el log
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", List.of());
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus estado, String mensaje, List<String> errores) {
        return ResponseEntity.status(estado).body(new ErrorResponse(estado.value(), mensaje, errores));
    }
}