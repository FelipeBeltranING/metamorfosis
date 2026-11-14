package com.metamorfosis.backend.dto;

/**
 * HU29: cuánto le falta a una meta para llegar a su destino, según sus hitos.
 * {@code porcentaje} es null cuando la meta no tiene hitos (no se muestra un avance que no existe).
 */
public record AvanceResponse(int logrados, int total, Integer porcentaje) {

    public static AvanceResponse de(int logrados, int total) {
        return new AvanceResponse(logrados, total, total == 0 ? null : (int) Math.round(100.0 * logrados / total));
    }

    /** Todos los hitos están logrados: la app propone marcar la meta como cumplida (HU6). */
    public boolean completo() {
        return total > 0 && logrados == total;
    }
}
