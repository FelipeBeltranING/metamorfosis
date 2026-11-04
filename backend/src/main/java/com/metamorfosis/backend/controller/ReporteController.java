package com.metamorfosis.backend.controller;

import com.metamorfosis.backend.dto.ReporteResponse;
import com.metamorfosis.backend.service.ReporteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /** HU7: estadísticas del reporte general del usuario autenticado. */
    @GetMapping("/resumen")
    public ResponseEntity<ReporteResponse> resumen(@AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(reporteService.resumen(codigoUsuario));
    }
}
