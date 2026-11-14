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
    CAMBIAR_VISIBILIDAD, // HU25: ocultar o mostrar una meta en el perfil
    COMPARTIR_LOGRO,    // HU21
    LISTAR_LOGROS,      // HU21 (Mis logros, ya retirada): se conserva para leer las filas viejas del log
    VER_REPORTE,        // HU7
    VER_PERFIL,         // HU22
    VER_INSIGNIAS,      // HU12
    BUSCAR_USUARIOS,    // HU24: búsqueda de personas por nombre
    VER_PERFIL_PUBLICO, // HU24: perfil de otra persona
    CAMBIAR_PRIVACIDAD, // HU26: perfil privado o público
    REGISTRAR_USUARIO,  // HU8
    SOLICITAR_REGISTRO, // HU8: se envió el correo de autorización
    RECHAZAR_REGISTRO,  // HU8: el dueño del correo respondió "No fui yo"
    INICIAR_SESION,     // HU9
    CERRAR_SESION,      // HU10
    CREAR_SUB_META,     // HU28: agregar un hito o un hábito al camino
    EDITAR_SUB_META,    // HU28: editar un paso del camino
    ELIMINAR_SUB_META,  // HU28: eliminar un paso del camino
    REGISTRAR_DIA,      // HU5: registrar o corregir el día de un hábito
    QUITAR_REGISTRO,    // HU5: quitar el registro de un día
    LOGRAR_HITO,        // HU5: marcar un hito del camino como logrado
    DESHACER_HITO,      // HU5: devolver un hito a pendiente
    EDITAR_PERFIL,      // HU23: nombre y apellido
    CAMBIAR_FOTO_PERFIL, // HU23
    QUITAR_FOTO_PERFIL, // HU23
    CAMBIAR_APARIENCIA, // HU19: tema, modo o color de acento
    INVITAR_META,       // HU13: invitar a una persona a la meta
    VER_INVITACIONES,   // HU14: invitaciones pendientes
    RESPONDER_INVITACION, // HU14: aceptar o rechazar (desde la app o el correo)
    GENERAR_CODIGO_META, // código para unirse a la meta
    DESACTIVAR_CODIGO_META,
    UNIRSE_CON_CODIGO,
    EDITAR_PERFIL_PUBLICO // HU19: frase, portada, insignia y metas destacadas
}
