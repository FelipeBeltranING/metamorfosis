package com.metamorfosis.backend.service;

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
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HU6: caja blanca de MetaService.marcarCumplida (marcar una meta, sin poder deshacerlo). */
@ExtendWith(MockitoExtension.class)
class MetaServiceMarcarCumplidaTest {

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

        MetaResponse r = metaService.marcarCumplida(CODIGO_META, CREADOR);

        assertEquals(EstadoMeta.CUMPLIDA, r.estado());
        assertEquals(original, r.fechaCumplida());
        verify(metaRepository, never()).save(any());
    }

    @Test
    void marcarDejaLaMetaCumplidaConFechaDeHoy() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(metaRepository.save(meta)).thenReturn(meta);

        MetaResponse r = metaService.marcarCumplida(CODIGO_META, CREADOR);

        assertEquals(EstadoMeta.CUMPLIDA, r.estado());
        assertEquals(LocalDate.now(), r.fechaCumplida());
        assertEquals(LocalDate.now(), meta.getFechaCumplida(CREADOR));
        verify(metaRepository, times(1)).save(meta);
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
}
