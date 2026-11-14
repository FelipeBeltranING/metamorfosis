package com.metamorfosis.backend.config;

import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.TokenRevocadoRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import com.metamorfosis.backend.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** HU32: el filtro del JWT rechaza los tokens emitidos antes del último cambio de contraseña. */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private TokenRevocadoRepository tokenRevocadoRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private final JwtService jwtService =
            new JwtService("secreto-de-prueba-largo-y-aleatorio-para-hs256-0123456789abcdef", 60);
    private JwtAuthFilter filtro;
    private String token;

    @BeforeEach
    void preparar() {
        filtro = new JwtAuthFilter(jwtService, tokenRevocadoRepository, usuarioRepository);
        Usuario usuario = new Usuario("Daniel", "Santos", "daniel@correo.com", "hash");
        usuario.setCodigoUsuario(1);
        token = jwtService.generarToken(usuario);
        when(tokenRevocadoRepository.existsByJti(anyString())).thenReturn(false);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private Object autenticado() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/metas");
        peticion.addHeader("Authorization", "Bearer " + token);
        filtro.doFilter(peticion, new MockHttpServletResponse(), new MockFilterChain());
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion == null ? null : autenticacion.getPrincipal();
    }

    private static LocalDateTime ahoraUtc() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    }

    @Test
    void sinCambioDeContrasenaElTokenSirve() throws Exception {
        when(usuarioRepository.contrasenaCambiadaEn(1)).thenReturn(Optional.empty());

        assertEquals(1, autenticado());
    }

    @Test
    void unTokenEmitidoAntesDelCambioYaNoSirve() throws Exception {
        when(usuarioRepository.contrasenaCambiadaEn(1)).thenReturn(Optional.of(ahoraUtc().plusMinutes(1)));

        assertNull(autenticado());
    }

    @Test
    void elTokenEmitidoEnElMismoSegundoDelCambioSirve() throws Exception {
        LocalDateTime emision = jwtService.extraerEmision(token);
        when(usuarioRepository.contrasenaCambiadaEn(1)).thenReturn(Optional.of(emision));

        assertEquals(1, autenticado());
    }

    @Test
    void unTokenEmitidoDespuesDelCambioSirve() throws Exception {
        when(usuarioRepository.contrasenaCambiadaEn(1)).thenReturn(Optional.of(ahoraUtc().minusDays(2)));

        assertEquals(1, autenticado());
    }
}
