package com.metamorfosis.backend.model;

/**
 * Acciones que quedan registradas en el log de actividad.
 * Al implementar una HU nueva se agrega aquí su valor; la tabla no cambia.
 */
public enum AccionLog {
    CREAR_META,         // HU1
    EDITAR_META,        // HU2
    LISTAR_METAS,       // HU3
    ELIMINAR_META,      // HU4
    REGISTRAR_USUARIO   // HU8
}
