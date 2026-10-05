package com.metamorfosis.backend.controller;

import com.metamorfosis.backend.dto.CrearMetaRequest;
import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.service.MetaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/metas")
public class MetaController {

    private final MetaService metaService;

    public MetaController(MetaService metaService) {
        this.metaService = metaService;
    }

    /** TEMPORAL: codigoUsuario llega por parámetro hasta que exista el login (saldrá del JWT). */
    @PostMapping
    public ResponseEntity<MetaResponse> crearMeta(@RequestBody CrearMetaRequest solicitud,
                                                  @RequestParam Integer codigoUsuario) {
        MetaResponse creada = metaService.crearMeta(solicitud, codigoUsuario);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .replaceQuery(null).path("/{id}").buildAndExpand(creada.codigoMeta()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }
}