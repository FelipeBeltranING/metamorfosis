package com.metamorfosis.backend.dto;

import com.metamorfosis.backend.model.EstadoMeta;

/**
 * HU27: estado por el que se filtra la lista de metas (parámetro ?estado= de GET /api/metas).
 * ACTIVA es el valor por defecto, así la lista de Mis metas se ve igual que antes.
 */
public enum FiltroEstado {
    ACTIVA, CUMPLIDA, TODAS;

    /** true si una meta con ese estado entra en el filtro. */
    public boolean incluye(EstadoMeta estado) {
        return switch (this) {
            case ACTIVA -> estado == EstadoMeta.ACTIVA;
            case CUMPLIDA -> estado == EstadoMeta.CUMPLIDA;
            case TODAS -> true;
        };
    }
}
