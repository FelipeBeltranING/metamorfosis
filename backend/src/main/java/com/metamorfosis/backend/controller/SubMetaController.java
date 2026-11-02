package com.metamorfosis.backend.controller;

import com.metamorfosis.backend.dto.CaminoResponse;
import com.metamorfosis.backend.dto.DiaResponse;
import com.metamorfosis.backend.dto.EstadisticasResponse;
import com.metamorfosis.backend.dto.RegistrarDiaRequest;
import com.metamorfosis.backend.dto.ResumenCaminoResponse;
import com.metamorfosis.backend.dto.SubMetaRequest;
import com.metamorfosis.backend.dto.SubMetaResponse;
import com.metamorfosis.backend.service.CaminoService;
import com.metamorfosis.backend.service.RegistroDiaService;
import com.metamorfosis.backend.service.SubMetaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Sub-metas: el camino de cada meta (sub-metas diarias, registro del día y estadísticas). */
@RestController
@RequestMapping("/api")
public class SubMetaController {

    private final SubMetaService subMetaService;
    private final RegistroDiaService registroDiaService;
    private final CaminoService caminoService;

    public SubMetaController(SubMetaService subMetaService, RegistroDiaService registroDiaService,
                             CaminoService caminoService) {
        this.subMetaService = subMetaService;
        this.registroDiaService = registroDiaService;
        this.caminoService = caminoService;
    }

    /** HU3: constancia y sub-metas de hoy de cada meta activa, para las tarjetas de "Mis metas". */
    @GetMapping("/metas/camino-hoy")
    public ResponseEntity<List<ResumenCaminoResponse>> resumenes(@AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(caminoService.resumenes(codigoUsuario));
    }

    /** "Tu camino" de una meta. */
    @GetMapping("/metas/{codigoMeta}/camino")
    public ResponseEntity<CaminoResponse> camino(@PathVariable Integer codigoMeta,
                                                 @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(caminoService.camino(codigoMeta, codigoUsuario));
    }

    @PostMapping("/metas/{codigoMeta}/submetas")
    public ResponseEntity<SubMetaResponse> crear(@PathVariable Integer codigoMeta, @RequestBody SubMetaRequest pedido,
                                                 @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subMetaService.crear(codigoMeta, pedido, codigoUsuario));
    }

    @PutMapping("/submetas/{codigoSubMeta}")
    public ResponseEntity<SubMetaResponse> editar(@PathVariable Integer codigoSubMeta, @RequestBody SubMetaRequest pedido,
                                                  @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(subMetaService.editar(codigoSubMeta, pedido, codigoUsuario));
    }

    @DeleteMapping("/submetas/{codigoSubMeta}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer codigoSubMeta,
                                         @AuthenticationPrincipal Integer codigoUsuario) {
        subMetaService.eliminar(codigoSubMeta, codigoUsuario);
        return ResponseEntity.noContent().build();
    }

    /** Registra o corrige el día (fecha en formato AAAA-MM-DD). */
    @PutMapping("/submetas/{codigoSubMeta}/registros/{fecha}")
    public ResponseEntity<DiaResponse> registrar(@PathVariable Integer codigoSubMeta,
                                                 @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                                 @RequestBody RegistrarDiaRequest pedido,
                                                 @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(registroDiaService.registrar(codigoSubMeta, fecha, pedido, codigoUsuario));
    }

    @DeleteMapping("/submetas/{codigoSubMeta}/registros/{fecha}")
    public ResponseEntity<Void> quitar(@PathVariable Integer codigoSubMeta,
                                       @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                       @AuthenticationPrincipal Integer codigoUsuario) {
        registroDiaService.quitar(codigoSubMeta, fecha, codigoUsuario);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/submetas/{codigoSubMeta}/estadisticas")
    public ResponseEntity<EstadisticasResponse> estadisticas(@PathVariable Integer codigoSubMeta,
                                                             @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(caminoService.estadisticas(codigoSubMeta, codigoUsuario));
    }
}
