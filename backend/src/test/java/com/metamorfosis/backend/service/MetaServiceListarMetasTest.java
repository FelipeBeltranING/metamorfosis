package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.dto.FiltroEstado;
import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/** HU27: caja blanca de MetaService.listarMetas (filtros por estado, tipo y plazo). */
@ExtendWith(MockitoExtension.class)
class MetaServiceListarMetasTest {

    private static final Integer USUARIO = 1;

    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private MetaService metaService;

    // 1 Leer (ACTIVA, ACADEMICA, CORTO) · 2 Ahorrar (ACTIVA, FINANCIERA, LARGO)
    // 3 Correr (CUMPLIDA, SALUD_BIENESTAR, CORTO) · 4 Curso (CUMPLIDA, ACADEMICA, MEDIANO)
    private List<Meta> metas;

    @BeforeEach
    void setUp() throws Exception {
        Usuario usuario = new Usuario("Felipe", "Beltran", "felipe@test.com", "hash");
        usuario.setCodigoUsuario(USUARIO);
        metas = List.of(
                meta(1, "Leer 20 libros", TipoMeta.ACADEMICA, Plazo.CORTO, usuario, false),
                meta(2, "Ahorrar para la casa", TipoMeta.FINANCIERA, Plazo.LARGO, usuario, false),
                meta(3, "Correr 5 km", TipoMeta.SALUD_BIENESTAR, Plazo.CORTO, usuario, true),
                meta(4, "Terminar el curso", TipoMeta.ACADEMICA, Plazo.MEDIANO, usuario, true));
    }

    private static Meta meta(int codigo, String nombre, TipoMeta tipo, Plazo plazo,
                             Usuario creador, boolean cumplida) throws Exception {
        Meta m = new Meta(nombre, plazo, tipo);
        m.crear(creador);
        if (cumplida) m.marcarCumplida(creador.getCodigoUsuario(), LocalDate.of(2026, 10, 30));
        Field id = Meta.class.getDeclaredField("codigoMeta");
        id.setAccessible(true);
        id.set(m, codigo);
        return m;
    }

    private void usuarioConMetas() {
        when(usuarioRepository.existsById(USUARIO)).thenReturn(true);
        when(metaRepository.findByParticipante(USUARIO)).thenReturn(metas);
    }

    private static List<Integer> codigos(List<MetaResponse> r) {
        return r.stream().map(MetaResponse::codigoMeta).toList();
    }

    @Test
    void usuarioInexistenteLanzaExcepcionSinConsultarMetas() {
        when(usuarioRepository.existsById(USUARIO)).thenReturn(false);

        assertThrows(UsuarioNoEncontradoException.class,
                () -> metaService.listarMetas(USUARIO, null, null, null));
        verify(metaRepository, never()).findByParticipante(anyInt());
    }

    @Test
    void sinFiltrosDevuelveSoloLasActivas() {
        usuarioConMetas();

        List<MetaResponse> r = metaService.listarMetas(USUARIO, null, null, null);

        assertEquals(List.of(1, 2), codigos(r));
        assertTrue(r.stream().allMatch(m -> m.estado() == EstadoMeta.ACTIVA));
    }

    @Test
    void estadoCumplidaDevuelveSoloLasCumplidasConSuFecha() {
        usuarioConMetas();

        List<MetaResponse> r = metaService.listarMetas(USUARIO, FiltroEstado.CUMPLIDA, null, null);

        assertEquals(List.of(3, 4), codigos(r));
        assertTrue(r.stream().allMatch(m -> LocalDate.of(2026, 10, 30).equals(m.fechaCumplida())));
    }

    @Test
    void estadoTodasDevuelveActivasYCumplidas() {
        usuarioConMetas();

        assertEquals(List.of(1, 2, 3, 4), codigos(metaService.listarMetas(USUARIO, FiltroEstado.TODAS, null, null)));
    }

    @Test
    void filtraPorTipo() {
        usuarioConMetas();

        assertEquals(List.of(1, 4),
                codigos(metaService.listarMetas(USUARIO, FiltroEstado.TODAS, TipoMeta.ACADEMICA, null)));
    }

    @Test
    void filtraPorPlazo() {
        usuarioConMetas();

        assertEquals(List.of(1, 3),
                codigos(metaService.listarMetas(USUARIO, FiltroEstado.TODAS, null, Plazo.CORTO)));
    }

    @Test
    void combinaLosTresFiltros() {
        usuarioConMetas();

        assertEquals(List.of(1),
                codigos(metaService.listarMetas(USUARIO, FiltroEstado.ACTIVA, TipoMeta.ACADEMICA, Plazo.CORTO)));
    }

    @Test
    void sinCoincidenciasDevuelveListaVacia() {
        usuarioConMetas();

        assertTrue(metaService.listarMetas(USUARIO, FiltroEstado.CUMPLIDA, TipoMeta.FINANCIERA, null).isEmpty());
    }

    @Test
    void elLogGuardaLosFiltrosUsados() {
        usuarioConMetas();

        try (MockedStatic<ContextoAuditoria> contexto = mockStatic(ContextoAuditoria.class)) {
            metaService.listarMetas(USUARIO, null, TipoMeta.ACADEMICA, null);

            contexto.verify(() -> ContextoAuditoria.agregar("filtroEstado", FiltroEstado.ACTIVA));
            contexto.verify(() -> ContextoAuditoria.agregar("filtroTipo", TipoMeta.ACADEMICA));
            contexto.verify(() -> ContextoAuditoria.agregar(eq("filtroPlazo"), any()), never());
        }
    }

    @Test
    void filtroEstadoIncluyeSegunElEstadoDeLaMeta() {
        assertTrue(FiltroEstado.ACTIVA.incluye(EstadoMeta.ACTIVA));
        assertFalse(FiltroEstado.ACTIVA.incluye(EstadoMeta.CUMPLIDA));
        assertTrue(FiltroEstado.CUMPLIDA.incluye(EstadoMeta.CUMPLIDA));
        assertFalse(FiltroEstado.CUMPLIDA.incluye(EstadoMeta.ACTIVA));
        assertTrue(FiltroEstado.TODAS.incluye(EstadoMeta.ACTIVA));
        assertTrue(FiltroEstado.TODAS.incluye(EstadoMeta.CUMPLIDA));
    }
}
