package com.metamorfosis.backend.auditoria;

import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.ResultadoLog;
import com.metamorfosis.backend.service.LogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Intercepta los métodos marcados con {@link Auditar} y guarda el log de actividad.
 * Se ejecuta por FUERA de la transacción del servicio (máxima precedencia), de modo que
 * ve tanto los éxitos como las excepciones. Un fallo al guardar el log nunca rompe la acción.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuditoriaAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaAspect.class);

    private final LogService logService;

    public AuditoriaAspect(LogService logService) {
        this.logService = logService;
    }

    @Around("@annotation(com.metamorfosis.backend.auditoria.Auditar)")
    public Object auditar(ProceedingJoinPoint pjp) throws Throwable {
        Method metodo = ((MethodSignature) pjp.getSignature()).getMethod();
        Auditar auditar = AnnotatedElementUtils.findMergedAnnotation(metodo, Auditar.class);
        if (auditar == null) {
            return pjp.proceed();
        }

        ContextoAuditoria.limpiar();
        try {
            Object resultado = pjp.proceed();
            guardar(pjp, auditar, ResultadoLog.EXITO, resultado, null);
            return resultado;
        } catch (Throwable e) {
            guardar(pjp, auditar, ResultadoLog.ERROR, null, e);
            throw e;
        } finally {
            ContextoAuditoria.limpiar();
        }
    }

    private void guardar(ProceedingJoinPoint pjp, Auditar auditar, ResultadoLog resultado,
                         Object retorno, Throwable error) {
        try {
            Map<String, Object> detalle = new LinkedHashMap<>();
            if (error != null) {
                detalle.put("error", error.getClass().getSimpleName());
                detalle.put("mensaje", error.getMessage());
                if (error instanceof ValidacionException v) detalle.put("errores", v.getErrores());
            } else if (retorno instanceof MetaResponse m) {
                detalle.put("nombre", m.nombre());
                detalle.put("plazo", m.plazo());
                detalle.put("tipo", m.tipo());
                detalle.put("estado", m.estado());
            } else if (retorno instanceof Collection<?> c) {
                detalle.put("cantidad", c.size());
            }
            detalle.putAll(ContextoAuditoria.obtener());

            logService.registrar(
                    parametroEntero(pjp, "codigoUsuario"),   // TEMPORAL: con el JWT (HU9) saldrá del usuario autenticado
                    auditar.accion(),
                    auditar.entidad(),
                    codigoEntidad(pjp, auditar.entidad(), retorno),
                    resultado,
                    aJson(detalle));
        } catch (Exception e) {
            log.error("No se pudo guardar el log de actividad", e);
        }
    }

    /** Cuál registro se tocó: el id de la respuesta (crear) o el parámetro codigoMeta (editar). */
    private Integer codigoEntidad(ProceedingJoinPoint pjp, EntidadLog entidad, Object retorno) {
        if (entidad == EntidadLog.META) {
            if (retorno instanceof MetaResponse m) return m.codigoMeta();
            return parametroEntero(pjp, "codigoMeta");
        }
        return null;
    }

    private Integer parametroEntero(ProceedingJoinPoint pjp, String nombre) {
        String[] nombres = ((MethodSignature) pjp.getSignature()).getParameterNames();
        Object[] valores = pjp.getArgs();
        if (nombres == null) return null;
        for (int i = 0; i < nombres.length; i++) {
            if (nombre.equals(nombres[i]) && valores[i] instanceof Integer n) return n;
        }
        return null;
    }

    // ---- JSON mínimo (evita depender de la versión de Jackson) ----
    private String aJson(Object valor) {
        if (valor == null) return "null";
        if (valor instanceof Number || valor instanceof Boolean) return valor.toString();
        if (valor instanceof Map<?, ?> mapa) {
            StringBuilder sb = new StringBuilder("{");
            boolean primero = true;
            for (Map.Entry<?, ?> e : mapa.entrySet()) {
                if (!primero) sb.append(',');
                primero = false;
                sb.append(aJson(String.valueOf(e.getKey()))).append(':').append(aJson(e.getValue()));
            }
            return sb.append('}').toString();
        }
        if (valor instanceof Iterable<?> it) {
            StringBuilder sb = new StringBuilder("[");
            boolean primero = true;
            for (Object o : it) {
                if (!primero) sb.append(',');
                primero = false;
                sb.append(aJson(o));
            }
            return sb.append(']').toString();
        }
        String s = valor.toString();
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"') sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c == '\t') sb.append("\\t");
            else if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
            else sb.append(c);
        }
        return sb.append('"').toString();
    }
}
