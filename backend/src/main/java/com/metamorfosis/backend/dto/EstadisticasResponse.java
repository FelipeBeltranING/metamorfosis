package com.metamorfosis.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Estadísticas de una sub-meta para el usuario que consulta. Porcentajes de 0 a 100 (null si no hay
 * días que contar). {@code porDiaSemana}: % por día, lunes primero (null si ese día no toca).
 * {@code dias}: desde que se creó (máximo un año) hasta hoy, del más antiguo al más reciente.
 */
public record EstadisticasResponse(
        SubMetaResponse subMeta,
        String nombreMeta,
        boolean soloLectura,
        LocalDate hoy,
        Integer constancia7,
        Integer constancia30,
        int rachaActual,
        int mejorRacha,
        List<Racha> mejoresRachas,
        int diasCumplidos,
        int diasRegistrados,
        BigDecimal total,
        BigDecimal promedio,
        List<Integer> porDiaSemana,
        List<DiaResponse> dias
) {
    public record Racha(LocalDate inicio, LocalDate fin, int dias) {}
}
