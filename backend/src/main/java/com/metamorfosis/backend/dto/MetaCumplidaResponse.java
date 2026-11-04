package com.metamorfosis.backend.dto;

import java.util.List;

/**
 * HU6 + HU11: respuesta de PATCH /api/metas/{codigoMeta}/cumplida. Trae la meta y las insignias
 * que el usuario ganó al cumplirla (lista vacía si no ganó ninguna).
 */
public record MetaCumplidaResponse(MetaResponse meta, List<InsigniaResponse> insigniasNuevas) { }
