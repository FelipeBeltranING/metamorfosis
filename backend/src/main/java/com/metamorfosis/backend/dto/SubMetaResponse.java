package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.SubMeta;
import com.metamorfosis.backend.model.TipoObjetivo;
import com.metamorfosis.backend.model.TipoSubMeta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SubMetaResponse(
        Integer codigoSubMeta,
        Integer codigoMeta,
        String nombre,
        String pregunta,
        TipoSubMeta tipo,
        String unidad,
        BigDecimal objetivo,
        TipoObjetivo tipoObjetivo,
        List<Integer> dias,
        String notas,
        LocalDate fechaCreacion
) {
    public static SubMetaResponse desde(SubMeta s) {
        return new SubMetaResponse(s.getCodigoSubMeta(), s.getMeta().getCodigoMeta(), s.getNombre(),
                s.getPregunta(), s.getTipo(), s.getUnidad(), s.getObjetivo(), s.getTipoObjetivo(),
                s.getDiasSemana(), s.getNotas(), s.getFechaCreacion());
    }
}
