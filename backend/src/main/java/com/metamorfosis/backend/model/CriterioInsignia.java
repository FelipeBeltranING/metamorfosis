package com.metamorfosis.backend.model;

/** HU11: qué cuenta una insignia para saber si el usuario la alcanzó. */
public enum CriterioInsignia {
    /** Total de metas cumplidas. */
    METAS_CUMPLIDAS,
    /** Metas cumplidas de un plazo (plazoRequerido). */
    METAS_CUMPLIDAS_PLAZO,
    /** Cantidad de tipos de meta distintos entre las metas cumplidas. */
    TIPOS_DISTINTOS
}
