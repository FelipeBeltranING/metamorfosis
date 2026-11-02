package com.metamorfosis.backend.dto;

import java.util.List;

/** Para la tarjeta de "Mis metas" (HU3): constancia de 7 días y las sub-metas que tocan hoy. */
public record ResumenCaminoResponse(
        Integer codigoMeta,
        int totalSubMetas,
        long totalRegistros,
        Integer constancia7,
        int cumplidasHoy,
        int tocanHoy,
        List<HoyItem> hoy
) {
    /** Una sub-meta que toca hoy, con lo registrado (valor null = pendiente). */
    public record HoyItem(SubMetaResponse subMeta, DiaResponse dia) {}
}
