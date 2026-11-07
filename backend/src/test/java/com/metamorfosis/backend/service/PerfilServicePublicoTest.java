package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.dto.PerfilPublicoResponse;
import com.metamorfosis.backend.dto.UsuarioResumenResponse;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.model.Usuario;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** HU24: caja blanca de PerfilService.buscarUsuarios, verPerfilPublico y patronDeBusqueda. */
@ExtendWith(MockitoExtension.class)
class PerfilServicePublicoTest {

    private static final Integer LAURA = 1;
    private static final Integer MATEO = 2;

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioInsigniaRepository usuarioInsigniaRepository;
    @Mock private InsigniaRepository insigniaRepository;

    @InjectMocks
    private PerfilService perfilService;

    private Usuario laura;

    @BeforeEach
    void setUp() {
        laura = usuario(LAURA, "Laura", "Gómez");
        laura.setFechaRegistro(LocalDate.of(2026, 9, 20));
    }

    private static Usuario usuario(Integer codigo, String nombre, String apellido) {
        Usuario u = new Usuario(nombre, apellido, nombre.toLowerCase() + "@correo.com", "hash");
        u.setCodigoUsuario(codigo);
        return u;
    }

    private Meta meta(int codigo, LocalDate cumplida, boolean oculta) throws Exception {
        Meta m = new Meta("Meta " + codigo, Plazo.CORTO, TipoMeta.PERSONAL);
        m.crear(laura);
        Field f = Meta.class.getDeclaredField("codigoMeta");
        f.setAccessible(true);
        f.set(m, codigo);
        if (cumplida != null) m.marcarCumplida(LAURA, cumplida);
        if (oculta) m.cambiarVisibilidad(LAURA, true);
        return m;
    }

    // ---- Patrón de búsqueda ----

    @Test
    void textoNuloOCortoLanza400() {
        for (String texto : Arrays.asList(null, "", " l ", "%_")) {
            ValidacionException e = assertThrows(ValidacionException.class,
                    () -> PerfilService.patronDeBusqueda(texto));
            assertEquals(List.of("Escribe al menos 2 letras para buscar"), e.getErrores());
        }
    }

    @Test
    void elPatronQuedaSinMayusculasTildesNiEspaciosDeMas() {
        assertEquals("%gomez%", PerfilService.patronDeBusqueda("  GÓMEZ "));
        assertEquals("%laura gomez%", PerfilService.patronDeBusqueda("Laura   Gómez"));
        assertEquals("%nunez%", PerfilService.patronDeBusqueda("Núñez"));
    }

    @Test
    void seQuitanLosComodines() {
        assertEquals("%lau%", PerfilService.patronDeBusqueda("la%u_"));
    }

    // ---- Búsqueda ----

    @Test
    void busquedaCortaNoConsultaElRepositorio() {
        assertThrows(ValidacionException.class, () -> perfilService.buscarUsuarios("l", MATEO));
        verify(usuarioRepository, never()).buscarPorNombre(anyInt(), anyString(), any(Pageable.class));
    }

    @Test
    void laBusquedaDevuelvePersonasSinCorreoYExcluyeAQuienBusca() {
        Usuario laureano = usuario(3, "Laureano", "Pérez");
        when(usuarioRepository.buscarPorNombre(eq(MATEO), eq("%lau%"), any(Pageable.class)))
                .thenReturn(List.of(laura, laureano));

        try (MockedStatic<ContextoAuditoria> contexto = mockStatic(ContextoAuditoria.class)) {
            List<UsuarioResumenResponse> resultado = perfilService.buscarUsuarios("Lau", MATEO);

            assertEquals(List.of(LAURA, 3), resultado.stream().map(UsuarioResumenResponse::codigoUsuario).toList());
            assertEquals("Laura", resultado.get(0).nombre());
            contexto.verify(() -> ContextoAuditoria.agregar("busqueda", "lau"));
        }
        assertTrue(Arrays.stream(UsuarioResumenResponse.class.getRecordComponents())
                .map(RecordComponent::getName).noneMatch("email"::equals));
    }

    @Test
    void laBusquedaPideMaximoVeintePersonas() {
        when(usuarioRepository.buscarPorNombre(eq(MATEO), eq("%lau%"), any(Pageable.class))).thenReturn(List.of());

        assertTrue(perfilService.buscarUsuarios("lau", MATEO).isEmpty());
        verify(usuarioRepository).buscarPorNombre(MATEO, "%lau%", PageRequest.of(0, 20));
    }

    // ---- Perfil público ----

    @Test
    void personaInexistenteLanza404() {
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> perfilService.verPerfilPublico(99, MATEO));
        verify(metaRepository, never()).findByParticipante(anyInt());
    }

    @Test
    void lasMetasOcultasNoAparecenNiCuentan() throws Exception {
        LocalDate hoy = LocalDate.of(2026, 11, 3);
        List<Meta> metas = List.of(
                meta(10, null, false),
                meta(11, null, false),
                meta(12, hoy, true),                    // cumplida y oculta
                meta(13, hoy.minusDays(1), false),
                meta(14, hoy.minusDays(2), false));
        when(usuarioRepository.findById(LAURA)).thenReturn(Optional.of(laura));
        when(metaRepository.findByParticipante(LAURA)).thenReturn(metas);
        when(usuarioInsigniaRepository.insigniasDelUsuario(LAURA)).thenReturn(List.of());
        when(insigniaRepository.count()).thenReturn(6L);

        PerfilPublicoResponse perfil = perfilService.verPerfilPublico(LAURA, MATEO);

        assertEquals(4, perfil.resumen().totalMetas());
        assertEquals(2, perfil.resumen().metasActivas());
        assertEquals(2, perfil.resumen().metasCumplidas());
        assertEquals(50, perfil.resumen().porcentajeCumplimiento());
        assertEquals(List.of(13, 14), perfil.ultimasCumplidas().stream().map(MetaResponse::codigoMeta).toList());
        assertTrue(perfil.ultimasCumplidas().stream().noneMatch(MetaResponse::oculta));
    }

    @Test
    void sinMetasElResumenQuedaEnCero() {
        Usuario mateo = usuario(MATEO, "Mateo", "Ríos");
        when(usuarioRepository.findById(MATEO)).thenReturn(Optional.of(mateo));
        when(metaRepository.findByParticipante(MATEO)).thenReturn(List.of());
        when(usuarioInsigniaRepository.insigniasDelUsuario(MATEO)).thenReturn(List.of());
        when(insigniaRepository.count()).thenReturn(6L);

        PerfilPublicoResponse perfil = perfilService.verPerfilPublico(MATEO, LAURA);

        assertEquals(0, perfil.resumen().totalMetas());
        assertEquals(0, perfil.resumen().porcentajeCumplimiento());
        assertTrue(perfil.ultimasCumplidas().isEmpty());
        assertTrue(perfil.insignias().isEmpty());
        assertEquals(6, perfil.totalInsignias());
    }

    @Test
    void elPerfilPublicoNoLlevaCorreo() {
        when(usuarioRepository.findById(LAURA)).thenReturn(Optional.of(laura));
        when(metaRepository.findByParticipante(LAURA)).thenReturn(List.of());
        when(usuarioInsigniaRepository.insigniasDelUsuario(LAURA)).thenReturn(List.of());

        PerfilPublicoResponse perfil = perfilService.verPerfilPublico(LAURA, MATEO);

        assertEquals("Laura", perfil.nombre());
        assertEquals(LocalDate.of(2026, 9, 20), perfil.fechaRegistro());
        assertTrue(Arrays.stream(PerfilPublicoResponse.class.getRecordComponents())
                .map(RecordComponent::getName).noneMatch("email"::equals));
    }
}
