package com.metamorfosis.backend.service;

import com.metamorfosis.backend.controller.AuthController;
import com.metamorfosis.backend.exception.TokenInvalidoException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.ResultadoLog;
import com.metamorfosis.backend.model.TokenRevocado;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.TokenRevocadoRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * HU10: caja blanca de AuthController.cerrarSesion y AuthService.cerrarSesion. Son los 6 casos del plan de
 * pruebas del Sprint 1 (sección 8.8), que quedaron pendientes de ejecutar.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceCerrarSesionTest {

    private static final String TOKEN = "token.de.prueba";
    private static final String JTI = "jti-1";
    private static final Integer USUARIO = 7;
    private static final LocalDateTime EXPIRA = LocalDateTime.of(2026, 11, 14, 20, 0);

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TokenRevocadoRepository tokenRevocadoRepository;
    @Mock private LogService logService;
    @Mock private BloqueoLoginService bloqueoLoginService;
    @Mock private SolicitudRegistroService solicitudRegistroService;

    private AuthService servicio;

    @BeforeEach
    void preparar() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash-relleno");
        servicio = new AuthService(usuarioRepository, passwordEncoder, jwtService, tokenRevocadoRepository,
                logService, bloqueoLoginService, solicitudRegistroService);
    }

    private void tokenValido() {
        when(jwtService.extraerId(TOKEN)).thenReturn(JTI);
        when(jwtService.extraerExpiracion(TOKEN)).thenReturn(EXPIRA);
        when(jwtService.extraerCodigoUsuario(TOKEN)).thenReturn(USUARIO);
    }

    // CB1 · Controlador: decisión sobre la cabecera
    @Test
    void cb1ElControladorRevisaLaCabecera() {
        AuthService mock = mock(AuthService.class);
        AuthController controlador = new AuthController(mock, solicitudRegistroService);

        TokenInvalidoException nula = assertThrows(TokenInvalidoException.class, () -> controlador.cerrarSesion(null));
        assertEquals("Falta el token de autorización", nula.getMessage());
        assertThrows(TokenInvalidoException.class, () -> controlador.cerrarSesion("abc123"));
        verifyNoInteractions(mock);

        assertEquals(204, controlador.cerrarSesion("Bearer   " + TOKEN + "  ").getStatusCode().value());
        verify(mock).cerrarSesion(TOKEN); // el token llega recortado
    }

    // CB2 · Servicio: falla la extracción del token
    @Test
    void cb2TokenMalFormadoRegistraElFalloSinUsuarioYNoGuardaNada() {
        when(jwtService.extraerId(TOKEN)).thenThrow(new TokenInvalidoException("Token inválido o expirado"));

        assertThrows(TokenInvalidoException.class, () -> servicio.cerrarSesion(TOKEN));

        verify(logService).registrar(null, AccionLog.CERRAR_SESION, EntidadLog.USUARIO, null, ResultadoLog.ERROR,
                "{\"motivo\":\"TOKEN_INVALIDO\"}");
        verify(tokenRevocadoRepository, never()).save(any());
    }

    // CB3 · Servicio: existsByJti verdadero
    @Test
    void cb3TokenYaRevocadoNoSeGuardaOtraVez() {
        tokenValido();
        when(tokenRevocadoRepository.existsByJti(JTI)).thenReturn(true);

        TokenInvalidoException e = assertThrows(TokenInvalidoException.class, () -> servicio.cerrarSesion(TOKEN));

        assertEquals("La sesión ya fue cerrada", e.getMessage());
        verify(logService).registrar(USUARIO, AccionLog.CERRAR_SESION, EntidadLog.USUARIO, USUARIO, ResultadoLog.ERROR,
                "{\"motivo\":\"TOKEN_YA_REVOCADO\"}");
        verify(tokenRevocadoRepository, never()).save(any());
    }

    // CB4 · Servicio: save lanza DataIntegrityViolationException (dos cierres simultáneos)
    @Test
    void cb4DosCierresSimultaneosGanaElPrimero() {
        tokenValido();
        when(tokenRevocadoRepository.existsByJti(JTI)).thenReturn(false);
        when(tokenRevocadoRepository.save(any())).thenThrow(new DataIntegrityViolationException("jti duplicado"));

        TokenInvalidoException e = assertThrows(TokenInvalidoException.class, () -> servicio.cerrarSesion(TOKEN));

        assertEquals("La sesión ya fue cerrada", e.getMessage());
        verify(logService).registrar(USUARIO, AccionLog.CERRAR_SESION, EntidadLog.USUARIO, USUARIO, ResultadoLog.ERROR,
                "{\"motivo\":\"TOKEN_YA_REVOCADO\"}");
    }

    // CB5 · Servicio: ruta feliz
    @Test
    void cb5RutaFelizGuardaElJtiYRegistraExito() {
        tokenValido();
        when(tokenRevocadoRepository.existsByJti(JTI)).thenReturn(false);

        servicio.cerrarSesion(TOKEN);

        ArgumentCaptor<TokenRevocado> guardado = ArgumentCaptor.forClass(TokenRevocado.class);
        verify(tokenRevocadoRepository, times(1)).save(guardado.capture());
        assertEquals(JTI, guardado.getValue().getJti());
        assertEquals(EXPIRA, guardado.getValue().getFechaExpiracion());
        verify(logService).registrar(eq(USUARIO), eq(AccionLog.CERRAR_SESION), eq(EntidadLog.USUARIO), eq(USUARIO),
                eq(ResultadoLog.EXITO), any());
    }

    // CB6 · Fecha de expiración guardada en UTC (JwtService real)
    @Test
    void cb6LaExpiracionSeGuardaEnUtc() {
        JwtService real = new JwtService("secreto-de-pruebas-de-32-bytes-o-mas!!", 60);
        Usuario usuario = new Usuario("Ana", "Newman", "ana@test.com", "hash");
        usuario.setCodigoUsuario(USUARIO);
        Instant antes = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        String token = real.generarToken(usuario);
        LocalDateTime expira = real.extraerExpiracion(token);

        LocalDateTime esperada = LocalDateTime.ofInstant(antes.plus(60, ChronoUnit.MINUTES), ZoneOffset.UTC);
        long diferencia = Math.abs(ChronoUnit.SECONDS.between(esperada, expira));
        assertTrue(diferencia <= 2, "la expiración debe ser ahora + 60 min en UTC, diferencia: " + diferencia + " s");
    }
}
