package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.config.CalendarioUsuario;
import com.metamorfosis.backend.dto.ReporteResponse;
import com.metamorfosis.backend.dto.ReporteResponse.PorMes;
import com.metamorfosis.backend.dto.ReporteResponse.PorPlazo;
import com.metamorfosis.backend.dto.ReporteResponse.PorTipo;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.repository.ReporteRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/** HU7: caja blanca de ReporteService.resumen. "Hoy" es el 3 de noviembre de 2026. */
@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    private static final Integer USUARIO = 1;
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Mock private ReporteRepository reporteRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private ReporteService reporteService;

    @BeforeEach
    void setUp() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-03T15:00:00Z"), BOGOTA);
        reporteService = new ReporteService(reporteRepository, usuarioRepository, new CalendarioUsuario(reloj));
    }

    private static Object[] fila(Object clave, EstadoMeta estado, long cantidad) {
        return new Object[]{clave, estado, cantidad};
    }

    private void usuarioSinMetas() {
        when(usuarioRepository.existsById(USUARIO)).thenReturn(true);
        when(reporteRepository.contarPorTipoYEstado(USUARIO)).thenReturn(List.of());
        when(reporteRepository.contarPorPlazoYEstado(USUARIO)).thenReturn(List.of());
        when(reporteRepository.fechasCumplidasDesde(USUARIO, LocalDate.of(2026, 6, 1))).thenReturn(List.of());
    }

    private void usuarioConMetas() {
        when(usuarioRepository.existsById(USUARIO)).thenReturn(true);
        when(reporteRepository.contarPorTipoYEstado(USUARIO)).thenReturn(List.of(
                fila(TipoMeta.ACADEMICA, EstadoMeta.ACTIVA, 2),
                fila(TipoMeta.ACADEMICA, EstadoMeta.CUMPLIDA, 1),
                fila(TipoMeta.SALUD_BIENESTAR, EstadoMeta.CUMPLIDA, 3),
                fila(TipoMeta.FINANCIERA, EstadoMeta.ACTIVA, 1)));
        when(reporteRepository.contarPorPlazoYEstado(USUARIO)).thenReturn(List.of(
                fila(Plazo.CORTO, EstadoMeta.ACTIVA, 2),
                fila(Plazo.CORTO, EstadoMeta.CUMPLIDA, 3),
                fila(Plazo.LARGO, EstadoMeta.ACTIVA, 1),
                fila(Plazo.MEDIANO, EstadoMeta.CUMPLIDA, 1)));
        when(reporteRepository.fechasCumplidasDesde(USUARIO, LocalDate.of(2026, 6, 1))).thenReturn(List.of(
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 30), LocalDate.of(2026, 11, 3)));
    }

    @Test
    void usuarioInexistenteLanzaExcepcionSinConsultarElReporte() {
        when(usuarioRepository.existsById(USUARIO)).thenReturn(false);

        assertThrows(UsuarioNoEncontradoException.class, () -> reporteService.resumen(USUARIO));
        verify(reporteRepository, never()).contarPorTipoYEstado(anyInt());
    }

    @Test
    void sinMetasTodoEnCeroPeroConLosTresPlazosYLosSeisMeses() {
        usuarioSinMetas();

        ReporteResponse r = reporteService.resumen(USUARIO);

        assertEquals(0, r.totalMetas());
        assertEquals(0, r.porcentajeCumplimiento());
        assertTrue(r.porTipo().isEmpty());
        assertEquals(List.of(new PorPlazo(Plazo.CORTO, 0, 0), new PorPlazo(Plazo.MEDIANO, 0, 0),
                new PorPlazo(Plazo.LARGO, 0, 0)), r.porPlazo());
        assertEquals(List.of("2026-06", "2026-07", "2026-08", "2026-09", "2026-10", "2026-11"),
                r.cumplidasPorMes().stream().map(PorMes::mes).toList());
        assertTrue(r.cumplidasPorMes().stream().allMatch(m -> m.cumplidas() == 0));
    }

    @Test
    void totalesYPorcentajeGeneral() {
        usuarioConMetas();

        ReporteResponse r = reporteService.resumen(USUARIO);

        assertEquals(7, r.totalMetas());
        assertEquals(4, r.metasCumplidas());
        assertEquals(3, r.metasActivas());
        assertEquals(57, r.porcentajeCumplimiento());   // 4 / 7 = 57,1 %
    }

    @Test
    void porTipoSoloIncluyeTiposConMetasEnElOrdenDelEnum() {
        usuarioConMetas();

        assertEquals(List.of(
                new PorTipo(TipoMeta.FINANCIERA, 1, 0, 0),
                new PorTipo(TipoMeta.SALUD_BIENESTAR, 3, 3, 100),
                new PorTipo(TipoMeta.ACADEMICA, 3, 1, 33)), reporteService.resumen(USUARIO).porTipo());
    }

    @Test
    void porPlazoSeparaActivasYCumplidas() {
        usuarioConMetas();

        assertEquals(List.of(
                new PorPlazo(Plazo.CORTO, 2, 3),
                new PorPlazo(Plazo.MEDIANO, 0, 1),
                new PorPlazo(Plazo.LARGO, 1, 0)), reporteService.resumen(USUARIO).porPlazo());
    }

    @Test
    void cumplidasPorMesCuentaLasFechasDeLosUltimosSeisMeses() {
        usuarioConMetas();

        assertEquals(List.of(
                new PorMes("2026-06", 1), new PorMes("2026-07", 0), new PorMes("2026-08", 0),
                new PorMes("2026-09", 0), new PorMes("2026-10", 2), new PorMes("2026-11", 1)),
                reporteService.resumen(USUARIO).cumplidasPorMes());
    }

    @Test
    void consultaLasFechasDesdeElPrimerDiaDelMesMasAntiguo() {
        usuarioSinMetas();

        reporteService.resumen(USUARIO);

        verify(reporteRepository).fechasCumplidasDesde(USUARIO, LocalDate.of(2026, 6, 1));
    }

    @Test
    void elLogGuardaTotalYCumplidas() {
        usuarioConMetas();

        try (MockedStatic<ContextoAuditoria> contexto = mockStatic(ContextoAuditoria.class)) {
            reporteService.resumen(USUARIO);

            contexto.verify(() -> ContextoAuditoria.agregar("totalMetas", 7L));
            contexto.verify(() -> ContextoAuditoria.agregar("metasCumplidas", 4L));
        }
    }

    @Test
    void porcentajeRedondeaYEvitaDividirPorCero() {
        assertEquals(0, ReporteService.porcentaje(0, 0));
        assertEquals(67, ReporteService.porcentaje(2, 3));
        assertEquals(33, ReporteService.porcentaje(1, 3));
        assertEquals(100, ReporteService.porcentaje(5, 5));
    }
}
