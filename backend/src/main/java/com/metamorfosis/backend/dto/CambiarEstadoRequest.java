package com.metamorfosis.backend.dto;

/** HU6: cuerpo de PATCH /api/metas/{codigoMeta}/cumplida. true = cumplida, false = activa. */
public record CambiarEstadoRequest(Boolean cumplida) { }
