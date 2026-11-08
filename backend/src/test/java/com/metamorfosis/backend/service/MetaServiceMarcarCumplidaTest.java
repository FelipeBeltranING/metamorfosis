package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.dto.InsigniaResponse;
import com.metamorfosis.backend.dto.MetaCumplidaResponse;
import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.exception.MetaNoEncontradaException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** HU6 y HU11: caja blanca de MetaService.marcarCumplida (marcar una meta y revisar insignias). */
@ExtendWith(MockitoExtension.class)
class MetaServiceMarcarCumplidaTest {

    private static final Integer CREADOR = 1;
    private static final Integer OTRO = 2;
    private static final Integer CODIGO_META = 10;

    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private InsigniaService insigniaService;

    @InjectMocks
    private MetaService metaService;

    private Meta meta;

    @BeforeEach
    void setUp() throws Exception {
        Usuario creador = new Usuario("Felipe", "Beltran", "felipe@test.com", "hash");
        creador.setCodigoUsuario(CREADOR);

        meta = new Meta("Leer 20 páginas", Plazo.CORTO, TipoMeta.ACADEMICA);
        meta.crear(creador); // deja la meta ACTIVA con la participación del creador
        Field id = Meta.class.getDeclaredField("codigoMeta");
        id.setAccessible(true);
        id.set(meta, CODIGO_META);
    }

    @Test
    void metaInexistenteLanza404() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.empty());

        assertThrows(MetaNoEncontradaException.class,
                () -> metaService.marcarCumplida(CODIGO_META, CREADOR));
        verify(metaRepository, never()).save(any());
    }

    @Test
    void usuarioQueNoEsElCreadorLanza403() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        PermisoDenegadoException e = assertThrows(PermisoDenegadoException.class,
                () -> metaService.marcarCumplida(CODIGO_META, OTRO));
        assertEquals("Solo el creador puede marcar la meta como cumplida", e.getMessage());
        assertEquals(EstadoMeta.ACTIVA, meta.getEstado());
        verify(metaRepository, never()).save(any());
    }

    @Test
    void elPermisoSeRevisaAntesQueElEstado() {
        meta.marcarCumplida(CREADOR, LocalDate.now());
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        assertThrows(PermisoDenegadoException.class,
                () -> metaService.marcarCumplida(CODIGO_META, OTRO));
    }

    @Test
    void marcarUnaMetaYaCumplidaNoCambiaNadaYConservaLaFecha() {
        LocalDate original = LocalDate.now().minusDays(3);
        meta.marcarCumplida(CREADOR, original);
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        MetaCumplidaResponse r = metaService.marcarCumplida(CODIGO_META, CREADOR);

        assertEquals(EstadoMeta.CUMPLIDA, r.meta().estado());
        assertEquals(original, r.meta().fechaCumplida());
        assertTrue(r.insigniasNuevas().isEmpty());
        verify(metaRepository, never()).save(any());
        verify(insigniaService, never()).otorgarNuevas(anyInt(), any()); // HU11: no se revisan insignias
    }

    @Test
    void marcarDejaLaMetaCumplidaConFechaDeHoy() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(metaRepository.save(meta)).thenReturn(meta);

        when(insigniaService.otorgarNuevas(CREADOR, LocalDate.now())).thenReturn(List.of());

        MetaCumplidaResponse r = metaService.marcarCumplida(CODIGO_META, CREADOR);

        assertEquals(EstadoMeta.CUMPLIDA, r.meta().estado());
        assertEquals(LocalDate.now(), r.meta().fechaCumplida());
        assertEquals(LocalDate.now(), meta.getFechaCumplida(CREADOR));
        assertTrue(r.insigniasNuevas().isEmpty());
        verify(metaRepository, times(1)).save(meta);
    }

    @Test
    void alMarcarSeRevisanLasInsigniasYQuedanEnElLog() {
        InsigniaResponse primerPaso = new InsigniaResponse(1, "Primer paso", "Cumpliste tu primera meta", "Footprints");
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(metaRepository.save(meta)).thenReturn(meta);
        when(insigniaService.otorgarNuevas(CREADOR, LocalDate.now())).thenReturn(List.of(primerPaso));

        try (MockedStatic<ContextoAuditoria> contexto = mockStatic(ContextoAuditoria.class)) {
            MetaCumplidaResponse r = metaService.marcarCumplida(CODIGO_META, CREADOR);

            assertEquals(List.of(primerPaso), r.insigniasNuevas());
            contexto.verify(() -> ContextoAuditoria.agregar("insigniasNuevas", List.of("Primer paso")));
        }
        verify(insigniaService, times(1)).otorgarNuevas(CREADOR, LocalDate.now());
    }

    @Test
    void conMetaAjenaNoSeRevisanInsignias() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        assertThrows(PermisoDenegadoException.class,
                () -> metaService.marcarCumplida(CODIGO_META, OTRO));
        verifyNoInteractions(insigniaService);
    }

    @Test
    void laRespuestaTraeLaFechaDeLaParticipacionDelCreador() {
        LocalDate fecha = LocalDate.of(2026, 10, 30);
        meta.marcarCumplida(CREADOR, fecha);

        MetaResponse r = MetaResponse.desde(meta);

        assertEquals(EstadoMeta.CUMPLIDA, r.estado());
        assertEquals(fecha, r.fechaCumplida());
    }

    @Test
    void unaMetaActivaNoTieneFechaDeCumplimiento() {
        MetaResponse r = MetaResponse.desde(meta);

        assertEquals(EstadoMeta.ACTIVA, r.estado());
        assertNull(r.fechaCumplida());
    }

    /** Meta compartida: se cumple para todos los aceptados, con la misma fecha, y cada uno puede ganar insignias. */
    @Test
    void enUnaMetaCompartidaSeCumpleParaTodosLosQueAceptaron() {
        Usuario laura = new Usuario("Laura", "Gómez", "laura@test.com", "hash");
        laura.setCodigoUsuario(2);
        Usuario andres = new Usuario("Andrés", "Rojas", "andres@test.com", "hash");
        andres.setCodigoUsuario(3);
        meta.invitar(laura, CREADOR, "h1", java.time.LocalDateTime.now(), java.time.LocalDateTime.now().plusDays(7))
                .responder(true);
        meta.invitar(andres, CREADOR, "h2", java.time.LocalDateTime.now(), java.time.LocalDateTime.now().plusDays(7));
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(metaRepository.save(meta)).thenReturn(meta);
        when(insigniaService.otorgarNuevas(anyInt(), any())).thenReturn(List.of());

        metaService.marcarCumplida(CODIGO_META, CREADOR);

        assertEquals(LocalDate.now(), meta.getFechaCumplida(2));
        assertNull(meta.getFechaCumplida(3)); // la invitación pendiente no cuenta
        verify(insigniaService).otorgarNuevas(CREADOR, LocalDate.now());
        verify(insigniaService).otorgarNuevas(2, LocalDate.now());
        verify(insigniaService, never()).otorgarNuevas(eq(3), any());
    }
}
