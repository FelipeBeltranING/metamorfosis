package com.metamorfosis.backend.controller;

import com.metamorfosis.backend.dto.LogroCompartidoResponse;
import com.metamorfosis.backend.dto.LogroPublicoResponse;
import com.metamorfosis.backend.service.LogroCompartidoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU21: compartir un logro y verlo desde el enlace público. */
@RestController
public class LogroController {

    private final LogroCompartidoService logroCompartidoService;

    public LogroController(LogroCompartidoService logroCompartidoService) {
        this.logroCompartidoService = logroCompartidoService;
    }

    /** Crea (o reutiliza) el enlace del logro. El usuario sale del token JWT. */
    @PostMapping("/api/metas/{codigoMeta}/compartir")
    public ResponseEntity<LogroCompartidoResponse> compartirLogro(
            @PathVariable Integer codigoMeta,
            @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(logroCompartidoService.compartirLogro(codigoMeta, codigoUsuario));
    }

    /** Página pública del logro: no requiere sesión (ver SeguridadConfig). */
    @GetMapping("/api/logros/{token}")
    public ResponseEntity<LogroPublicoResponse> verLogro(@PathVariable String token) {
        return ResponseEntity.ok(logroCompartidoService.obtenerLogroPublico(token));
    }
}
