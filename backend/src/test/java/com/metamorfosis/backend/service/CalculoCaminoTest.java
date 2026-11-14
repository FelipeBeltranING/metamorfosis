package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.DiaResponse;
import com.metamorfosis.backend.dto.EstadisticasResponse.Racha;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.SubMeta;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.model.TipoObjetivo;
import com.metamorfosis.backend.model.TipoSubMeta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HU5 y HU29: caja blanca de CalculoCamino, las reglas de los hábitos (qué días tocan, constancia y
 * rachas). No usa base de datos ni mocks.
 */
class CalculoCaminoTest {

    /** Lunes 2 de noviembre de 2026. */
    private static final LocalDate HOY = LocalDate.of(2026, 11, 2);
    private static final BigDecimal SI = BigDecimal.ONE;
    private static final BigDecimal NO = BigDecimal.ZERO;

    private final Meta meta = new Meta("Leer 20 libros", Plazo.MEDIANO, TipoMeta.PERSONAL);

    private SubMeta siNo(int... diasIso) {
        SubMeta s = new SubMeta(meta, TipoSubMeta.SI_NO, HOY.minusDays(60));
        int dias = 0;
        for (int d : diasIso) dias |= SubMeta.bitDe(DayOfWeek.of(d));
        s.actualizar("Leer antes de dormir", null, null, null, null,
                diasIso.length == 0 ? SubMeta.TODOS_LOS_DIAS : dias, null);
        return s;
    }

    private SubMeta medible(TipoObjetivo tipoObjetivo, String objetivo) {
        SubMeta s = new SubMeta(meta, TipoSubMeta.MEDIBLE, HOY.minusDays(60));
        s.actualizar("Páginas", null, "págs", new BigDecimal(objetivo), tipoObjetivo, SubMeta.TODOS_LOS_DIAS, null);
        return s;
    }

    /** Valores de los últimos días: el último elemento es hoy (null = sin registro). */
    private static Map<LocalDate, BigDecimal> valores(BigDecimal... ultimos) {
        Map<LocalDate, BigDecimal> mapa = new HashMap<>();
        for (int i = 0; i < ultimos.length; i++) {
            BigDecimal v = ultimos[i];
            if (v != null) mapa.put(HOY.minusDays(ultimos.length - 1L - i), v);
        }
        return mapa;
    }

    private static List<DiaResponse> dias(SubMeta s, Map<LocalDate, BigDecimal> valores, int n) {
        return CalculoCamino.dias(s, valores, HOY.minusDays(n - 1L), HOY);
    }

    @Test
    void unHitoNuncaTocaNingunDia() {
        SubMeta hito = new SubMeta(meta, TipoSubMeta.HITO, HOY.minusDays(10));
        hito.actualizar("Terminar el libro 1", null, null, null, null, 0, null);

        assertTrue(dias(hito, Map.of(), 7).stream().noneMatch(DiaResponse::toca));
        assertEquals(SubMeta.TODOS_LOS_DIAS, hito.getDias(), "la columna dias no admite vacío");
    }

    @Test
    void soloTocanLosDiasMarcados() {
        SubMeta lunesAViernes = siNo(1, 2, 3, 4, 5);
        List<DiaResponse> semana = dias(lunesAViernes, Map.of(), 7); // martes 27 oct a lunes 2 nov

        assertEquals(List.of(true, true, true, true, false, false, true),
                semana.stream().map(DiaResponse::toca).toList());
    }

    @Test
    void noTocaAntesDeQueElHabitoExistiera() {
        SubMeta nuevo = new SubMeta(meta, TipoSubMeta.SI_NO, HOY);
        nuevo.actualizar("Caminar", null, null, null, null, SubMeta.TODOS_LOS_DIAS, null);

        List<DiaResponse> d = dias(nuevo, Map.of(), 3);
        assertFalse(d.get(0).toca());
        assertFalse(d.get(1).toca());
        assertTrue(d.get(2).toca());
    }

    @Test
    void medibleAlMenosCumpleDesdeElObjetivo() {
        SubMeta paginas = medible(TipoObjetivo.AL_MENOS, "15");
        assertTrue(paginas.cumple(new BigDecimal("15")));
        assertTrue(paginas.cumple(new BigDecimal("20")));
        assertFalse(paginas.cumple(new BigDecimal("14.99")));
    }

    @Test
    void medibleComoMaximoCumpleHastaElObjetivo() {
        SubMeta redes = medible(TipoObjetivo.COMO_MAXIMO, "60");
        assertTrue(redes.cumple(new BigDecimal("45")));
        assertTrue(redes.cumple(new BigDecimal("60")));
        assertFalse(redes.cumple(new BigDecimal("61")));
    }

    @Test
    void hoySinRegistrarNoCuentaComoFallado() {
        SubMeta h = siNo();
        List<DiaResponse> d = dias(h, valores(SI, SI, null), 3);

        assertEquals(100, CalculoCamino.constancia(d, HOY, 7));
        assertEquals(2, CalculoCamino.rachaActual(d, HOY));
    }

    @Test
    void constanciaEsCumplidosSobreDiasQueCuentan() {
        SubMeta h = siNo();
        // 4 días pasados: sí, no, sí, sin registro (cuenta como no) y hoy sí
        List<DiaResponse> d = dias(h, valores(SI, NO, SI, null, SI), 5);

        assertEquals(60, CalculoCamino.constancia(d, HOY, 7)); // 3 de 5
    }

    @Test
    void constanciaEsNullSiNoHayDiasQueContar() {
        SubMeta nuevo = new SubMeta(meta, TipoSubMeta.SI_NO, HOY);
        nuevo.actualizar("Caminar", null, null, null, null, SubMeta.TODOS_LOS_DIAS, null);

        assertNull(CalculoCamino.constancia(dias(nuevo, Map.of(), 7), HOY, 7));
    }

    @Test
    void losDiasQueNoTocanNoCortanLaRacha() {
        SubMeta lunesMiercolesViernes = siNo(1, 3, 5);
        // vie 30 oct sí, sáb y dom no tocan, lun 2 nov sí
        Map<LocalDate, BigDecimal> v = Map.of(HOY.minusDays(3), SI, HOY, SI);
        List<DiaResponse> d = dias(lunesMiercolesViernes, v, 4);

        assertEquals(2, CalculoCamino.rachaActual(d, HOY));
    }

    @Test
    void lasRachasSeOrdenanDeLaMasLargaALaMasCorta() {
        SubMeta h = siNo();
        List<DiaResponse> d = dias(h, valores(SI, SI, NO, SI, SI, SI, NO, SI), 8);

        List<Racha> rachas = CalculoCamino.rachas(d, HOY);
        assertEquals(List.of(3, 2, 1), rachas.stream().map(Racha::dias).toList());
        assertEquals(HOY, rachas.get(2).fin());
    }

    @Test
    void diasSeguidosCompletosExigeTodosLosHabitosQueTocan() {
        SubMeta a = siNo();
        SubMeta b = siNo();
        List<DiaResponse> da = dias(a, valores(SI, SI, SI), 3);
        List<DiaResponse> db = dias(b, valores(NO, SI, SI), 3);

        assertEquals(2, CalculoCamino.diasSeguidosCompletos(List.of(da, db), HOY));
    }

    @Test
    void porDiaSemanaEmpiezaEnLunesYEsNullSiNuncaToco() {
        SubMeta soloLunes = siNo(1);
        List<DiaResponse> d = dias(soloLunes, Map.of(HOY, SI, HOY.minusDays(7), NO), 14);

        List<Integer> porDia = CalculoCamino.porDiaSemana(d, HOY);
        assertEquals(50, porDia.get(0));
        assertNull(porDia.get(1));
    }
}
