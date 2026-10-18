package com.metamorfosis.backend.auditoria;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Permite que un servicio agregue datos extra al log de la acción en curso
 * (por ejemplo el valor ANTERIOR de lo que se edita). Vive solo durante la petición.
 */
public final class ContextoAuditoria {

    private static final ThreadLocal<Map<String, Object>> EXTRA =
            ThreadLocal.withInitial(LinkedHashMap::new);

    private ContextoAuditoria() { }

    public static void agregar(String clave, Object valor) {
        EXTRA.get().put(clave, valor);
    }

    static Map<String, Object> obtener() {
        return new LinkedHashMap<>(EXTRA.get());
    }

    static void limpiar() {
        EXTRA.remove();
    }
}
