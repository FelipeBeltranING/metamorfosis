package com.metamorfosis.backend.dto;

import java.util.List;

/**
 * Para la tarjeta de "Mis metas" (HU3): el avance por hitos (HU29), la constancia de 7 días de los
 * hábitos y los hábitos que tocan hoy (HU5). {@code totalSubMetas} cuenta hitos y hábitos (HU4 lo usa
 * para avisar qué se borra con la meta).
 */
public record ResumenCaminoResponse(
        Integer codigoMeta,
        int totalSubMetas,
        AvanceResponse avance,
        long totalRegistros,
        Integer constancia7,
        int cumplidasHoy,
        int tocanHoy,
        List<HoyItem> hoy
) {
    /** Un hábito que toca hoy, con lo registrado (valor null = pendiente). */
    public record HoyItem(SubMetaResponse subMeta, DiaResponse dia) {}
}
