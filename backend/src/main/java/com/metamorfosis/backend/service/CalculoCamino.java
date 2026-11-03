package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.DiaResponse;
import com.metamorfosis.backend.dto.EstadisticasResponse.Racha;
import com.metamorfosis.backend.model.SubMeta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Sub-metas: las reglas del camino en un solo lugar (sin base de datos, fáciles de probar).
 * <ul>
 *   <li>Un día <b>toca</b> si su día de la semana está en la sub-meta y la sub-meta ya existía.</li>
 *   <li>Un día <b>cuenta</b> para los porcentajes si toca y ya pasó, o si es hoy y ya se registró
 *       (hoy sin registrar está pendiente, no es un día fallado).</li>
 *   <li><b>Constancia</b> = días cumplidos ÷ días que cuentan, en los últimos N días.</li>
 *   <li><b>Racha</b> = días seguidos que cuentan y se cumplieron; los días que no tocan no la cortan.</li>
 * </ul>
 */
public final class CalculoCamino {

    private CalculoCamino() { }

    /** Los días de una sub-meta entre {@code desde} y {@code hasta}, del más antiguo al más reciente. */
    public static List<DiaResponse> dias(SubMeta subMeta, Map<LocalDate, BigDecimal> valores,
                                         LocalDate desde, LocalDate hasta) {
        List<DiaResponse> dias = new ArrayList<>();
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            boolean toca = subMeta.tocaEl(d) && !d.isBefore(subMeta.getFechaCreacion());
            BigDecimal valor = valores.get(d);
            dias.add(new DiaResponse(d, toca, valor, valor != null && subMeta.cumple(valor)));
        }
        return dias;
    }

    public static boolean cuenta(DiaResponse dia, LocalDate hoy) {
        return dia.toca() && (dia.fecha().isBefore(hoy) || dia.valor() != null);
    }

    /** % de días cumplidos entre los que cuentan en los últimos {@code n} días; null si no hay ninguno. */
    public static Integer constancia(List<DiaResponse> dias, LocalDate hoy, int n) {
        LocalDate desde = hoy.minusDays(n - 1L);
        int cuentan = 0;
        int cumplidos = 0;
        for (DiaResponse d : dias) {
            if (d.fecha().isBefore(desde) || d.fecha().isAfter(hoy) || !cuenta(d, hoy)) continue;
            cuentan++;
            if (d.cumplido()) cumplidos++;
        }
        return porcentaje(cumplidos, cuentan);
    }

    /** Días seguidos cumplidos hasta hoy (los días del más antiguo al más reciente). */
    public static int rachaActual(List<DiaResponse> diasAsc, LocalDate hoy) {
        int racha = 0;
        for (int i = diasAsc.size() - 1; i >= 0; i--) {
            DiaResponse d = diasAsc.get(i);
            if (!cuenta(d, hoy)) continue;
            if (!d.cumplido()) break;
            racha++;
        }
        return racha;
    }

    /** Todas las rachas, de la más larga a la más corta (empate: la más reciente primero). */
    public static List<Racha> rachas(List<DiaResponse> diasAsc, LocalDate hoy) {
        List<Racha> rachas = new ArrayList<>();
        LocalDate inicio = null;
        LocalDate fin = null;
        int largo = 0;
        for (DiaResponse d : diasAsc) {
            if (!cuenta(d, hoy)) continue;
            if (d.cumplido()) {
                if (inicio == null) inicio = d.fecha();
                fin = d.fecha();
                largo++;
            } else if (inicio != null) {
                rachas.add(new Racha(inicio, fin, largo));
                inicio = null;
                largo = 0;
            }
        }
        if (inicio != null) rachas.add(new Racha(inicio, fin, largo));
        rachas.sort(Comparator.comparingInt(Racha::dias).reversed()
                .thenComparing(Racha::fin, Comparator.reverseOrder()));
        return rachas;
    }

    /** % de cumplimiento por día de la semana, lunes primero; null si ese día nunca contó. */
    public static List<Integer> porDiaSemana(List<DiaResponse> dias, LocalDate hoy) {
        int[] cuentan = new int[7];
        int[] cumplidos = new int[7];
        for (DiaResponse d : dias) {
            if (!cuenta(d, hoy)) continue;
            int i = d.fecha().getDayOfWeek().getValue() - 1;
            cuentan[i]++;
            if (d.cumplido()) cumplidos[i]++;
        }
        List<Integer> resultado = new ArrayList<>();
        for (int i = 0; i < 7; i++) resultado.add(porcentaje(cumplidos[i], cuentan[i]));
        return resultado;
    }

    /**
     * Días seguidos (hacia atrás desde hoy) en que se cumplieron TODAS las sub-metas que tocaban.
     * Los días en que no tocaba ninguna no cortan la cuenta; hoy solo cuenta si ya está completo.
     */
    public static int diasSeguidosCompletos(List<List<DiaResponse>> diasPorSubMeta, LocalDate hoy) {
        if (diasPorSubMeta.isEmpty()) return 0;
        int largo = diasPorSubMeta.get(0).size();
        int seguidos = 0;
        for (int i = largo - 1; i >= 0; i--) {
            int tocan = 0;
            int cumplidos = 0;
            boolean pendienteHoy = false;
            for (List<DiaResponse> dias : diasPorSubMeta) {
                DiaResponse d = dias.get(i);
                if (!d.toca()) continue;
                tocan++;
                if (d.cumplido()) cumplidos++;
                else if (d.fecha().equals(hoy) && d.valor() == null) pendienteHoy = true;
            }
            if (tocan == 0) continue;
            if (cumplidos == tocan) seguidos++;
            else if (pendienteHoy && cumplidos + contarPendientesHoy(diasPorSubMeta, i, hoy) == tocan) continue;
            else break;
        }
        return seguidos;
    }

    private static int contarPendientesHoy(List<List<DiaResponse>> diasPorSubMeta, int i, LocalDate hoy) {
        int pendientes = 0;
        for (List<DiaResponse> dias : diasPorSubMeta) {
            DiaResponse d = dias.get(i);
            if (d.toca() && d.fecha().equals(hoy) && d.valor() == null) pendientes++;
        }
        return pendientes;
    }

    static Integer porcentaje(int parte, int total) {
        return total == 0 ? null : (int) Math.round(100.0 * parte / total);
    }
}
