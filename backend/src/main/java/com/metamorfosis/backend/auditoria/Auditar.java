package com.metamorfosis.backend.auditoria;

import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un método de servicio para que su ejecución quede en el log de actividad
 * (quién, qué, sobre qué, cuándo y si salió bien o falló).
 * Uso: {@code @Auditar(accion = AccionLog.CREAR_META, entidad = EntidadLog.META)}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditar {
    AccionLog accion();
    EntidadLog entidad();
}
