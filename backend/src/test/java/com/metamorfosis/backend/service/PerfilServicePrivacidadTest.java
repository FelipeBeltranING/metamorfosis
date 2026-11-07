package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.dto.PerfilPublicoResponse;
import com.metamorfosis.backend.dto.PerfilResponse;
import com.metamorfosis.backend.dto.PrivacidadRequest;
import com.metamorfosis.backend.dto.PrivacidadResponse;
import com.metamorfosis.backend.dto.UsuarioResumenResponse;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/** HU26: caja blanca de PerfilService.cambiarPrivacidad y del perfil privado en verPerfilPublico. */
@ExtendWith(MockitoExtension.class)
class PerfilServicePrivacidadTest {

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
        laura = new Usuario("Laura", "Gómez", "laura@correo.com", "hash");
        laura.setCodigoUsuario(LAURA);
        laura.setFechaRegistro(LocalDate.of(2026, 9, 20));
        laura.setPerfilPrivado(false);
    }

    @Test
    void sinValorLanza400SinConsultarElUsuario() {
        for (PrivacidadRequest solicitud : new PrivacidadRequest[] { null, new PrivacidadRequest(null) }) {
            ValidacionException e = assertThrows(ValidacionException.class,
                    () -> perfilService.cambiarPrivacidad(solicitud, LAURA));
            assertEquals(List.of("Indica si el perfil queda privado"), e.getErrores());
        }
        verify(usuarioRepository, never()).findById(anyInt());
    }

    @Test
    void usuarioInexistenteLanza404() {
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> perfilService.cambiarPrivacidad(new PrivacidadRequest(true), 99));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void activarGuardaElPerfilPrivadoYLoRegistraEnElLog() {
        when(usuarioRepository.findById(LAURA)).thenReturn(Optional.of(laura));

        try (MockedStatic<ContextoAuditoria> contexto = mockStatic(ContextoAuditoria.class)) {
            PrivacidadResponse r = perfilService.cambiarPrivacidad(new PrivacidadRequest(true), LAURA);

            assertTrue(r.privado());
            contexto.verify(() -> ContextoAuditoria.agregar("privado", true));
        }
        assertTrue(laura.isPerfilPrivado());
        verify(usuarioRepository, times(1)).save(laura);
    }

    @Test
    void desactivarVuelveAPublico() {
        laura.setPerfilPrivado(true);
        when(usuarioRepository.findById(LAURA)).thenReturn(Optional.of(laura));

        PrivacidadResponse r = perfilService.cambiarPrivacidad(new PrivacidadRequest(false), LAURA);

        assertFalse(r.privado());
        assertFalse(laura.isPerfilPrivado());
    }

    @Test
    void unPerfilPrivadoSoloEnviaLaCabecera() {
        laura.setPerfilPrivado(true);
        when(usuarioRepository.findById(LAURA)).thenReturn(Optional.of(laura));
        when(insigniaRepository.count()).thenReturn(6L);

        PerfilPublicoResponse perfil = perfilService.verPerfilPublico(LAURA, MATEO);

        assertTrue(perfil.privado());
        assertEquals("Laura", perfil.nombre());
        assertEquals(LocalDate.of(2026, 9, 20), perfil.fechaRegistro());
        assertNull(perfil.resumen());
        assertTrue(perfil.insignias().isEmpty());
        assertTrue(perfil.ultimasCumplidas().isEmpty());
        verify(metaRepository, never()).findByParticipante(anyInt());
        verify(usuarioInsigniaRepository, never()).insigniasDelUsuario(anyInt());
    }

    @Test
    void unPerfilPublicoSigueIgualQueEnLaHu24() {
        when(usuarioRepository.findById(LAURA)).thenReturn(Optional.of(laura));
        when(metaRepository.findByParticipante(LAURA)).thenReturn(List.of());
        when(usuarioInsigniaRepository.insigniasDelUsuario(LAURA)).thenReturn(List.of());

        PerfilPublicoResponse perfil = perfilService.verPerfilPublico(LAURA, MATEO);

        assertFalse(perfil.privado());
        assertNotNull(perfil.resumen());
    }

    @Test
    void laBusquedaMarcaElPerfilPrivado() {
        laura.setPerfilPrivado(true);
        assertTrue(UsuarioResumenResponse.desde(laura).privado());
        laura.setPerfilPrivado(null);
        assertFalse(UsuarioResumenResponse.desde(laura).privado());
    }

    @Test
    void elDuenoVeTodoSuPerfilConElInterruptorEncendido() {
        laura.setPerfilPrivado(true);
        when(usuarioRepository.findById(LAURA)).thenReturn(Optional.of(laura));
        when(metaRepository.findByParticipante(LAURA)).thenReturn(List.of());
        when(usuarioInsigniaRepository.insigniasDelUsuario(LAURA)).thenReturn(List.of());

        PerfilResponse perfil = perfilService.verMiPerfil(LAURA);

        assertTrue(perfil.perfilPrivado());
        assertNotNull(perfil.resumen());
        assertEquals("laura@correo.com", perfil.email());
    }
}
