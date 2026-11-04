package com.metamorfosis.backend.controller;

import com.metamorfosis.backend.dto.CrearMetaRequest;
import com.metamorfosis.backend.dto.FiltroEstado;
import com.metamorfosis.backend.dto.MetaCumplidaResponse;
import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.service.MetaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/metas")
public class MetaController {

    private final MetaService metaService;

    public MetaController(MetaService metaService) {
        this.metaService = metaService;
    }

    /** HU1: crea una meta. El usuario sale del token JWT, no de la petición. */
    @PostMapping
    public ResponseEntity<MetaResponse> crearMeta(@RequestBody CrearMetaRequest solicitud,
                                                  @AuthenticationPrincipal Integer codigoUsuario) {
        MetaResponse creada = metaService.crearMeta(solicitud, codigoUsuario);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .replaceQuery(null).path("/{id}").buildAndExpand(creada.codigoMeta()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    /**
     * HU3 + HU27: metas del usuario autenticado, filtradas por ?estado=ACTIVA|CUMPLIDA|TODAS,
     * ?tipo= y ?plazo= (todos opcionales). Sin estado devuelve las activas.
     * Un valor que no existe responde 400 "Solicitud mal formada".
     */
    @GetMapping
    public ResponseEntity<List<MetaResponse>> listarMetas(
            @RequestParam(required = false) FiltroEstado estado,
            @RequestParam(required = false) TipoMeta tipo,
            @RequestParam(required = false) Plazo plazo,
            @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(metaService.listarMetas(codigoUsuario, estado, tipo, plazo));
    }

    /** HU2: edita una meta (solo su creador). */
    @PutMapping("/{codigoMeta}")
    public ResponseEntity<MetaResponse> editarMeta(@PathVariable Integer codigoMeta,
                                                   @RequestBody CrearMetaRequest solicitud,
                                                   @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(metaService.editarMeta(codigoMeta, solicitud, codigoUsuario));
    }

    /** HU4: elimina una meta (solo su creador). */
    @DeleteMapping("/{codigoMeta}")
    public ResponseEntity<Void> eliminarMeta(
            @PathVariable Integer codigoMeta,
            @AuthenticationPrincipal Integer codigoUsuario) {

        metaService.eliminarMeta(codigoMeta, codigoUsuario);

        return ResponseEntity.noContent().build();
    }

    /**
     * HU6: marca la meta como cumplida (solo su creador). No lleva cuerpo y no se puede deshacer.
     * HU11: la respuesta trae también las insignias que el usuario ganó.
     */
    @PatchMapping("/{codigoMeta}/cumplida")
    public ResponseEntity<MetaCumplidaResponse> marcarCumplida(@PathVariable Integer codigoMeta,
                                                       @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(metaService.marcarCumplida(codigoMeta, codigoUsuario));
    }
}
