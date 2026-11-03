package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.TipoObjetivo;
import com.metamorfosis.backend.model.TipoSubMeta;

import java.math.BigDecimal;
import java.util.List;

/**
 * Crear o editar una sub-meta. {@code dias}: números ISO (1 = lunes … 7 = domingo); vacío o null =
 * todos los días. En Sí/No se ignoran unidad, objetivo y tipo de objetivo. Al editar, el tipo no cambia.
 */
public record SubMetaRequest(
        String nombre,
        String pregunta,
        TipoSubMeta tipo,
        String unidad,
        BigDecimal objetivo,
        TipoObjetivo tipoObjetivo,
        List<Integer> dias,
        String notas
) {}
