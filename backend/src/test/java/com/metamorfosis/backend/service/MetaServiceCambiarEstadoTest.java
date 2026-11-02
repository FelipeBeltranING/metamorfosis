package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.dto.CambiarEstadoRequest;
import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.exception.MetaNoEncontradaException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.ValidacionException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HU6: caja blanca de MetaService.cambiarEstado (marcar / desmarcar una meta). */
@ExtendWith(MockitoExtension.class)
class MetaServiceCambiarEstadoTest {

    private static final Integer CREADOR = 1;
    private static final Integer OTRO = 2;
    private static final Integer CODIGO_META = 10;

    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioRepository usuarioRepository;

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

    // ---- validar ----

    @Test
    void solicitudNulaEsRechazadaSinConsultarLaBase() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> metaService.cambiarEstado(CODIGO_META, null, CREADOR));
        assertEquals("El campo cumplida es obligatorio", e.getErrores().get(0));
        verifyNoInteractions(metaRepository);
    }

    @Test
    void campoCumplidaNuloEsRechazadoSinConsultarLaBase() {
        assertThrows(ValidacionException.class,
                () -> metaService.cambiarEstado(CODIGO_META, new CambiarEstadoRequest(null), CREADOR));
        verifyNoInteractions(metaRepository);
    }

    // ---- existencia y permiso ----

    @Test
    void metaInexistenteLanza404() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.empty());

        assertThrows(MetaNoEncontradaException.class,
                () -> metaService.cambiarEstado(CODIGO_META, new CambiarEstadoRequest(true), CREADOR));
        verify(metaRepository, never()).save(any());
    }

    @Test
    void usuarioQueNoEsElCreadorLanza403() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        PermisoDenegadoException e = assertThrows(PermisoDenegadoException.class,
                () -> metaService.cambiarEstado(CODIGO_META, new CambiarEstadoRequest(true), OTRO));
        assertEquals("Solo el creador puede marcar la meta como cumplida", e.getMessage());
        assertEquals(EstadoMeta.ACTIVA, meta.getEstado());
        verify(metaRepository, never()).save(any());
    }

    @Test
    void elPermisoSeRevisaAntesQueElEstado() {
        meta.marcarCumplida(CREADOR, LocalDate.now());
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        assertThrows(PermisoDenegadoException.class,
                () -> metaService.cambiarEstado(CODIGO_META, new CambiarEstadoRequest(true), OTRO));
    }

    // ---- estado repetido: no cambia nada ----

    @Test
    void marcarUnaMetaYaCumplidaConservaLaFechaOriginal() {
        LocalDate original = LocalDate.now().minusDays(3);
        meta.marcarCumplida(CREADOR, original);
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        MetaResponse r = metaService.cambiarEstado(CODIGO_META, new CambiarEstadoRequest(true), CREADOR);

        assertEquals(EstadoMeta.CUMPLIDA, r.estado());
        assertEquals(original, r.fechaCumplida());
        verify(metaRepository, never()).save(any());
    }

    @Test
    void desmarcarUnaMetaYaActivaNoGuarda() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        MetaResponse r = metaService.cambiarEstado(CODIGO_META, new CambiarEstadoRequest(false), CREADOR);

        assertEquals(EstadoMeta.ACTIVA, r.estado());
        assertNull(r.fechaCumplida());
        verify(metaRepository, never()).save(any());
    }

    // ---- cambio de estado ----

    @Test
    void marcarDejaLaMetaCumplidaConFechaDeHoy() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(metaRepository.save(meta)).thenReturn(meta);

        try (MockedStatic<ContextoAuditoria> contexto = mockStatic(ContextoAuditoria.class)) {
            MetaResponse r = metaService.cambiarEstado(CODIGO_META, new CambiarEstadoRequest(true), CREADOR);

            assertEquals(EstadoMeta.CUMPLIDA, r.estado());
            assertEquals(LocalDate.now(), r.fechaCumplida());
            assertEquals(LocalDate.now(), meta.getFechaCumplida(CREADOR));
            verify(metaRepository, times(1)).save(meta);
            contexto.verify(() -> ContextoAuditoria.agregar("accion", "marcada"));
        }
    }

    @Test
    void desmarcarDevuelveLaMetaAActivaYBorraLaFecha() {
        meta.marcarCumplida(CREADOR, LocalDate.now().minusDays(1));
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(metaRepository.save(meta)).thenReturn(meta);

        try (MockedStatic<ContextoAuditoria> contexto = mockStatic(ContextoAuditoria.class)) {
            MetaResponse r = metaService.cambiarEstado(CODIGO_META, new CambiarEstadoRequest(false), CREADOR);

            assertEquals(EstadoMeta.ACTIVA, r.estado());
            assertNull(r.fechaCumplida());
            assertNull(meta.getFechaCumplida(CREADOR));
            verify(metaRepository, times(1)).save(meta);
            contexto.verify(() -> ContextoAuditoria.agregar("accion", "desmarcada"));
        }
    }

    // ---- MetaResponse ----

    @Test
    void laRespuestaTraeLaFechaDeLaParticipacionDelCreador() {
        LocalDate fecha = LocalDate.of(2026, 10, 30);
        meta.marcarCumplida(CREADOR, fecha);

        MetaResponse r = MetaResponse.desde(meta);

        assertEquals(EstadoMeta.CUMPLIDA, r.estado());
        assertEquals(fecha, r.fechaCumplida());
    }
}
