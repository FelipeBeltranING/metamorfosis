package com.metamorfosis.backend.model;

/**
 * Acciones que quedan registradas en el log de actividad.
 * Al implementar una HU nueva se agrega aquí su valor; la tabla no cambia.
 */
public enum AccionLog {
    CREAR_META,         // HU1
    EDITAR_META,        // HU2
    LISTAR_METAS,       // HU3 y HU27 (con filtros)
    ELIMINAR_META,      // HU4
    REGISTRAR_CUMPLIMIENTO, // HU5 (retirada; se conserva porque hay filas en log_actividad con este valor)
    MARCAR_CUMPLIDA,    // HU6
    COMPARTIR_LOGRO,    // HU21
    LISTAR_LOGROS,      // HU21 (Mis logros, ya retirada): se conserva para leer las filas viejas del log
    VER_REPORTE,        // HU7
    REGISTRAR_USUARIO,  // HU8
    SOLICITAR_REGISTRO, // HU8: se envió el correo de autorización
    RECHAZAR_REGISTRO,  // HU8: el dueño del correo respondió "No fui yo"
    INICIAR_SESION,     // HU9
    CERRAR_SESION,      // HU10
    CREAR_SUB_META,     // sub-metas: crear
    EDITAR_SUB_META,    // sub-metas: editar
    ELIMINAR_SUB_META,  // sub-metas: eliminar
    REGISTRAR_DIA,      // sub-metas: registrar o corregir el día
    QUITAR_REGISTRO,    // sub-metas: quitar el registro de un día
    EDITAR_PERFIL,      // HU23: nombre y apellido
    CAMBIAR_FOTO_PERFIL, // HU23
    QUITAR_FOTO_PERFIL  // HU23
}
