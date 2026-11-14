package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.CambiarContrasenaRequest;
import com.metamorfosis.backend.dto.ContrasenaCambiadaResponse;
import com.metamorfosis.backend.exception.CuentaBloqueadaException;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HU32: caja blanca de ContrasenaService.cambiar. */
@ExtendWith(MockitoExtension.class)
class ContrasenaServiceTest {

    private static final String ACTUAL = "Actual123";
    private static final String NUEVA = "Nueva2026x";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private BloqueoLoginService bloqueoLoginService;
    @Mock private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-14T20:15:30.750Z"), ZoneOffset.UTC);
    private ContrasenaService servicio;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        servicio = new ContrasenaService(usuarioRepository, passwordEncoder, bloqueoLoginService, jwtService, reloj);
        usuario = new Usuario("Daniel", "Santos", "Daniel@Correo.com", passwordEncoder.encode(ACTUAL));
        usuario.setCodigoUsuario(1);
    }

    private ValidacionException fallaCon(CambiarContrasenaRequest pedido) {
        return assertThrows(ValidacionException.class, () -> servicio.cambiar(1, pedido));
    }

    @Test
    void cambiaLaContrasenaAnotaElMomentoYDevuelveUnTokenNuevo() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generarToken(usuario)).thenReturn("token-nuevo");

        ContrasenaCambiadaResponse respuesta = servicio.cambiar(1, new CambiarContrasenaRequest(ACTUAL, NUEVA, NUEVA));

        assertEquals("token-nuevo", respuesta.token());
        assertTrue(passwordEncoder.matches(NUEVA, usuario.getPasswordHash()));
        assertFalse(passwordEncoder.matches(ACTUAL, usuario.getPasswordHash()));
        // En segundos enteros, como la fecha de emisión del JWT.
        assertEquals(LocalDateTime.of(2026, 11, 14, 20, 15, 30), usuario.getContrasenaCambiadaEn());
        verify(bloqueoLoginService).limpiar("daniel@correo.com");
    }

    @Test
    void laContrasenaActualIncorrectaCuentaComoIntentoFallido() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        ValidacionException e = fallaCon(new CambiarContrasenaRequest("Otra1234", NUEVA, NUEVA));

        assertEquals(List.of(ContrasenaService.MENSAJE_ACTUAL_INCORRECTA), e.getErrores());
        verify(bloqueoLoginService).registrarFallo("daniel@correo.com");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void conElCorreoBloqueadoNiSiquieraRevisaLaContrasena() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(bloqueoLoginService.segundosBloqueado("daniel@correo.com")).thenReturn(120L);

        CuentaBloqueadaException e = assertThrows(CuentaBloqueadaException.class,
                () -> servicio.cambiar(1, new CambiarContrasenaRequest(ACTUAL, NUEVA, NUEVA)));

        assertEquals(120L, e.getSegundosRestantes());
        verify(bloqueoLoginService, never()).registrarFallo(any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void laNuevaDebeSerDistintaDeLaActual() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        ValidacionException e = fallaCon(new CambiarContrasenaRequest(ACTUAL, ACTUAL, ACTUAL));

        assertEquals(List.of(ContrasenaService.MENSAJE_IGUAL_A_LA_ACTUAL), e.getErrores());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void sinDatosPideLasDosContrasenas() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        ValidacionException e = fallaCon(new CambiarContrasenaRequest("", null, null));

        assertEquals(List.of(ContrasenaService.MENSAJE_FALTA_ACTUAL, ContrasenaService.MENSAJE_FALTA_NUEVA),
                e.getErrores());
        verifyNoInteractions(bloqueoLoginService);
    }

    @Test
    void pedidoVacioTambienSeValida() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        ValidacionException e = fallaCon(null);

        assertEquals(2, e.getErrores().size());
    }

    @Test
    void laNuevaDebeCumplirLaReglaYCoincidirConLaConfirmacion() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        ValidacionException e = fallaCon(new CambiarContrasenaRequest(ACTUAL, "corta", "otra"));

        assertEquals(List.of(ReglaContrasena.MENSAJE, ContrasenaService.MENSAJE_NO_COINCIDEN), e.getErrores());
        verifyNoInteractions(bloqueoLoginService);
    }

    @Test
    void usuarioQueNoExiste() {
        when(usuarioRepository.findById(9)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> servicio.cambiar(9, new CambiarContrasenaRequest(ACTUAL, NUEVA, NUEVA)));
    }
}
