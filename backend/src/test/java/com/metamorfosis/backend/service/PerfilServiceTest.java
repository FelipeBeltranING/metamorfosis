package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.dto.PerfilResponse;
import com.metamorfosis.backend.dto.PerfilResponse.InsigniaObtenida;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.model.CriterioInsignia;
import com.metamorfosis.backend.model.Insignia;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.model.UsuarioInsignia;
import com.metamorfosis.backend.repository.InsigniaRepository;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.UsuarioInsigniaRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/** HU22: caja blanca de PerfilService.verMiPerfil. */
@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    private static final Integer USUARIO = 1;
    private static final long CATALOGO = 6;

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioInsigniaRepository usuarioInsigniaRepository;
    @Mock private InsigniaRepository insigniaRepository;

    @InjectMocks
    private PerfilService perfilService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Felipe", "Beltrán", "felipe@test.com", "hash");
        usuario.setCodigoUsuario(USUARIO);
        usuario.setFechaRegistro(LocalDate.of(2026, 9, 20));
    }

    private static void fijar(Object objeto, Class<?> clase, String campo, Object valor) throws Exception {
        Field f = clase.getDeclaredField(campo);
        f.setAccessible(true);
        f.set(objeto, valor);
    }

    private Meta activa(int codigo) throws Exception {
        Meta m = new Meta("Activa " + codigo, Plazo.CORTO, TipoMeta.ACADEMICA);
        m.crear(usuario);
        fijar(m, Meta.class, "codigoMeta", codigo);
        return m;
    }

    private Meta cumplida(int codigo, LocalDate fecha) throws Exception {
        Meta m = activa(codigo);
        m.marcarCumplida(USUARIO, fecha);
        return m;
    }

    private static UsuarioInsignia ganada(Usuario u, int codigo, String nombre, String icono, LocalDate fecha)
            throws Exception {
        Insignia i = new Insignia(nombre, "Descripción de " + nombre, icono, CriterioInsignia.METAS_CUMPLIDAS, null, 1);
        fijar(i, Insignia.class, "codigoInsignia", codigo);
        return new UsuarioInsignia(u, i, fecha);
    }

    private void usuarioCon(List<Meta> metas, List<UsuarioInsignia> insignias) {
        when(usuarioRepository.findById(USUARIO)).thenReturn(Optional.of(usuario));
        when(metaRepository.findByParticipante(USUARIO)).thenReturn(metas);
        when(usuarioInsigniaRepository.insigniasDelUsuario(USUARIO)).thenReturn(insignias);
        when(insigniaRepository.count()).thenReturn(CATALOGO);
    }

    private static List<Integer> codigos(List<MetaResponse> metas) {
        return metas.stream().map(MetaResponse::codigoMeta).toList();
    }

    @Test
    void usuarioInexistenteLanzaExcepcionSinConsultarMetasNiInsignias() {
        when(usuarioRepository.findById(USUARIO)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> perfilService.verMiPerfil(USUARIO));
        verify(metaRepository, never()).findByParticipante(anyInt());
        verify(usuarioInsigniaRepository, never()).insigniasDelUsuario(anyInt());
    }

    @Test
    void sinMetasElResumenQuedaEnCeroSinDividirPorCero() {
        usuarioCon(List.of(), List.of());

        PerfilResponse r = perfilService.verMiPerfil(USUARIO);

        assertEquals(new PerfilResponse.ResumenMetas(0, 0, 0, 0), r.resumen());
        assertTrue(r.ultimasCumplidas().isEmpty());
    }

    @Test
    void metasActivasYCumplidasDanElResumenYElPorcentaje() throws Exception {
        usuarioCon(List.of(activa(1), activa(2), activa(3),
                cumplida(4, LocalDate.of(2026, 11, 1)), cumplida(5, LocalDate.of(2026, 11, 2))), List.of());

        PerfilResponse r = perfilService.verMiPerfil(USUARIO);

        assertEquals(new PerfilResponse.ResumenMetas(5, 3, 2, 40), r.resumen());
    }

    @Test
    void lasCumplidasVanDeLaMasRecienteALaMasAntigua() throws Exception {
        usuarioCon(List.of(cumplida(1, LocalDate.of(2026, 11, 1)), cumplida(2, LocalDate.of(2026, 11, 3)),
                cumplida(3, LocalDate.of(2026, 11, 2))), List.of());

        PerfilResponse r = perfilService.verMiPerfil(USUARIO);

        assertEquals(List.of(2, 3, 1), codigos(r.ultimasCumplidas()));
        assertEquals(LocalDate.of(2026, 11, 3), r.ultimasCumplidas().get(0).fechaCumplida());
    }

    @Test
    void conMismaFechaVaPrimeroLaDeCodigoMayor() throws Exception {
        LocalDate dia = LocalDate.of(2026, 11, 2);
        usuarioCon(List.of(cumplida(7, dia), cumplida(9, dia)), List.of());

        assertEquals(List.of(9, 7), codigos(perfilService.verMiPerfil(USUARIO).ultimasCumplidas()));
    }

    @Test
    void soloSeMuestranLasTresCumplidasMasRecientes() throws Exception {
        List<Meta> metas = new ArrayList<>();
        for (int dia = 1; dia <= 5; dia++) metas.add(cumplida(dia, LocalDate.of(2026, 11, dia)));
        usuarioCon(metas, List.of());

        PerfilResponse r = perfilService.verMiPerfil(USUARIO);

        assertEquals(PerfilService.ULTIMAS_CUMPLIDAS, r.ultimasCumplidas().size());
        assertEquals(List.of(5, 4, 3), codigos(r.ultimasCumplidas()));
        assertEquals(5, r.resumen().metasCumplidas());
    }

    @Test
    void lasActivasNoAparecenEntreLasUltimasCumplidas() throws Exception {
        usuarioCon(List.of(activa(1), cumplida(2, LocalDate.of(2026, 11, 1))), List.of());

        assertEquals(List.of(2), codigos(perfilService.verMiPerfil(USUARIO).ultimasCumplidas()));
    }

    @Test
    void sinInsigniasLaListaVaVaciaYElTotalEsElCatalogo() {
        usuarioCon(List.of(), List.of());

        PerfilResponse r = perfilService.verMiPerfil(USUARIO);

        assertTrue(r.insignias().isEmpty());
        assertEquals(CATALOGO, r.totalInsignias());
    }

    @Test
    void conInsigniasSeRespetaElOrdenDeLaConsulta() throws Exception {
        usuarioCon(List.of(), List.of(
                ganada(usuario, 5, "Visión de largo plazo", "Mountain", LocalDate.of(2026, 11, 3)),
                ganada(usuario, 1, "Primer paso", "Footprints", LocalDate.of(2026, 11, 1))));

        List<InsigniaObtenida> insignias = perfilService.verMiPerfil(USUARIO).insignias();

        assertEquals(2, insignias.size());
        assertEquals(new InsigniaObtenida(5, "Visión de largo plazo", "Descripción de Visión de largo plazo",
                "Mountain", LocalDate.of(2026, 11, 3)), insignias.get(0));
        assertEquals("Primer paso", insignias.get(1).nombre());
        assertEquals("Footprints", insignias.get(1).icono());
        assertEquals(LocalDate.of(2026, 11, 1), insignias.get(1).fechaObtenida());
    }

    @Test
    void devuelveLosDatosPersonalesDelUsuario() {
        usuarioCon(List.of(), List.of());

        PerfilResponse r = perfilService.verMiPerfil(USUARIO);

        assertEquals("Felipe", r.nombre());
        assertEquals("Beltrán", r.apellido());
        assertEquals("felipe@test.com", r.email());
        assertEquals(LocalDate.of(2026, 9, 20), r.fechaRegistro());
    }

    @Test
    void dejaElResumenEnElContextoDeAuditoria() throws Exception {
        usuarioCon(List.of(activa(1), cumplida(2, LocalDate.of(2026, 11, 1))),
                List.of(ganada(usuario, 1, "Primer paso", "Footprints", LocalDate.of(2026, 11, 1))));

        try (MockedStatic<ContextoAuditoria> contexto = mockStatic(ContextoAuditoria.class)) {
            perfilService.verMiPerfil(USUARIO);

            contexto.verify(() -> ContextoAuditoria.agregar("totalMetas", 2L));
            contexto.verify(() -> ContextoAuditoria.agregar("metasCumplidas", 1L));
            contexto.verify(() -> ContextoAuditoria.agregar("insignias", 1));
        }
    }
}
