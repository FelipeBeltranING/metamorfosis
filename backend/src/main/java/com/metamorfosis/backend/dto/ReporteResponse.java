package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;

import java.util.List;

/**
 * HU7: reporte general de las metas del usuario.
 * porcentajeCumplimiento va de 0 a 100 (entero, redondeado).
 */
public record ReporteResponse(
        long totalMetas,
        long metasActivas,
        long metasCumplidas,
        int porcentajeCumplimiento,
        List<PorTipo> porTipo,
        List<PorPlazo> porPlazo,
        List<PorMes> cumplidasPorMes) {

    /** Metas de un tipo y qué porcentaje de ellas está cumplido. Solo tipos con al menos una meta. */
    public record PorTipo(TipoMeta tipo, long total, long cumplidas, int porcentaje) { }

    /** Metas activas y cumplidas de un plazo. Siempre los tres plazos. */
    public record PorPlazo(Plazo plazo, long activas, long cumplidas) { }

    /** Metas cumplidas en un mes ("2026-11"). Siempre los últimos seis meses, del más antiguo al actual. */
    public record PorMes(String mes, long cumplidas) { }
}
