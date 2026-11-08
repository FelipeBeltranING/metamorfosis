package com.metamorfosis.backend.dto;

import java.time.LocalDateTime;

/** Código para unirse a la meta (8 caracteres, se muestra como ABCD-EFGH) y hasta cuándo sirve. Ambos null: no hay código activo. */
public record CodigoUnionResponse(String codigo, LocalDateTime expira) {

    public static final CodigoUnionResponse NINGUNO = new CodigoUnionResponse(null, null);
}
