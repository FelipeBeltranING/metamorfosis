package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.Auditar;
import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.config.CalendarioUsuario;
import com.metamorfosis.backend.dto.ReporteResponse;
import com.metamorfosis.backend.dto.ReporteResponse.PorMes;
import com.metamorfosis.backend.dto.ReporteResponse.PorPlazo;
import com.metamorfosis.backend.dto.ReporteResponse.PorTipo;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.repository.ReporteRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** HU7: estadísticas del reporte general (sección 8 del documento). */
@Service
public class ReporteService {

    static final int MESES = 6;

    private final ReporteRepository reporteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalendarioUsuario calendario;

    public ReporteService(ReporteRepository reporteRepository, UsuarioRepository usuarioRepository,
                          CalendarioUsuario calendario) {
        this.reporteRepository = reporteRepository;
        this.usuarioRepository = usuarioRepository;
        this.calendario = calendario;
    }

    @Auditar(accion = AccionLog.VER_REPORTE, entidad = EntidadLog.META)
    @Transactional(readOnly = true)
    public ReporteResponse resumen(Integer codigoUsuario) {
        if (!usuarioRepository.existsById(codigoUsuario)) {
            throw new UsuarioNoEncontradoException(codigoUsuario);
        }

        // 1. Por tipo (y de ahí los totales generales).
        Map<TipoMeta, long[]> tipos = new EnumMap<>(TipoMeta.class);   // [total, cumplidas]
        long total = 0;
        long cumplidas = 0;
        for (Object[] fila : reporteRepository.contarPorTipoYEstado(codigoUsuario)) {
            TipoMeta tipo = (TipoMeta) fila[0];
            boolean cumplida = fila[1] == EstadoMeta.CUMPLIDA;
            long cantidad = ((Number) fila[2]).longValue();
            long[] conteo = tipos.computeIfAbsent(tipo, t -> new long[2]);
            conteo[0] += cantidad;
            total += cantidad;
            if (cumplida) {
                conteo[1] += cantidad;
                cumplidas += cantidad;
            }
        }
        List<PorTipo> porTipo = new ArrayList<>();
        tipos.forEach((tipo, c) -> porTipo.add(new PorTipo(tipo, c[0], c[1], porcentaje(c[1], c[0]))));

        // 2. Por plazo: siempre los tres, aunque no tengan metas.
        Map<Plazo, long[]> plazos = new EnumMap<>(Plazo.class);        // [activas, cumplidas]
        for (Plazo plazo : Plazo.values()) plazos.put(plazo, new long[2]);
        for (Object[] fila : reporteRepository.contarPorPlazoYEstado(codigoUsuario)) {
            long[] conteo = plazos.get((Plazo) fila[0]);
            conteo[fila[1] == EstadoMeta.CUMPLIDA ? 1 : 0] += ((Number) fila[2]).longValue();
        }
        List<PorPlazo> porPlazo = new ArrayList<>();
        plazos.forEach((plazo, c) -> porPlazo.add(new PorPlazo(plazo, c[0], c[1])));

        // 3. Cumplidas por mes: los últimos seis meses, incluido el actual, aunque estén en cero.
        YearMonth actual = YearMonth.from(calendario.hoy());
        YearMonth primero = actual.minusMonths(MESES - 1L);
        Map<YearMonth, Long> meses = new LinkedHashMap<>();
        for (int i = 0; i < MESES; i++) meses.put(primero.plusMonths(i), 0L);
        for (LocalDate fecha : reporteRepository.fechasCumplidasDesde(codigoUsuario, primero.atDay(1))) {
            meses.computeIfPresent(YearMonth.from(fecha), (mes, n) -> n + 1);
        }
        List<PorMes> porMes = new ArrayList<>();
        meses.forEach((mes, n) -> porMes.add(new PorMes(mes.toString(), n)));

        ContextoAuditoria.agregar("totalMetas", total);
        ContextoAuditoria.agregar("metasCumplidas", cumplidas);

        return new ReporteResponse(total, total - cumplidas, cumplidas, porcentaje(cumplidas, total),
                porTipo, porPlazo, porMes);
    }

    /** Porcentaje entero (0 a 100); 0 si no hay metas. */
    static int porcentaje(long parte, long total) {
        return total == 0 ? 0 : (int) Math.round(parte * 100.0 / total);
    }
}
