package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.CambiarContrasenaRequest;
import com.metamorfosis.backend.dto.ContrasenaCambiadaResponse;
import com.metamorfosis.backend.exception.CorreoNoEnviadoException;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.UsuarioRepository;
import com.metamorfosis.backend.service.correo.CorreoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** HU32: caja blanca de ContrasenaService.cambiar (dentro de la app: solo la nueva y su confirmación). */
@ExtendWith(MockitoExtension.class)
class ContrasenaServiceTest {

    private static final String ACTUAL = "Actual123";
    private static final String NUEVA = "Nueva2026x";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private BloqueoLoginService bloqueoLoginService;
    @Mock private JwtService jwtService;
    @Mock private CorreoService correoService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-14T20:15:30.750Z"), ZoneOffset.UTC);
    private ContrasenaService servicio;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        servicio = new ContrasenaService(usuarioRepository, passwordEncoder, bloqueoLoginService, jwtService,
                correoService, "https://metamorfosis.app", reloj);
        usuario = new Usuario("Daniel", "Santos", "Daniel@Correo.com", passwordEncoder.encode(ACTUAL));
        usuario.setCodigoUsuario(1);
        // Como lo deja Spring en una petición sin Accept-Language: español.
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));
    }

    @AfterEach
    void limpiarIdioma() {
        LocaleContextHolder.resetLocaleContext();
    }

    private void conUsuarioQueSeGuarda() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
    }

    private ValidacionException fallaCon(CambiarContrasenaRequest pedido) {
        return assertThrows(ValidacionException.class, () -> servicio.cambiar(1, pedido));
    }

    @Test
    void cambiaLaContrasenaAnotaElMomentoYDevuelveUnTokenNuevo() {
        conUsuarioQueSeGuarda();
        when(jwtService.generarToken(usuario)).thenReturn("token-nuevo");

        ContrasenaCambiadaResponse respuesta = servicio.cambiar(1, new CambiarContrasenaRequest(NUEVA, NUEVA));

        assertEquals("token-nuevo", respuesta.token());
        assertTrue(passwordEncoder.matches(NUEVA, usuario.getPasswordHash()));
        assertFalse(passwordEncoder.matches(ACTUAL, usuario.getPasswordHash()));
        // En segundos enteros, como la fecha de emisión del JWT.
        assertEquals(LocalDateTime.of(2026, 11, 14, 20, 15, 30), usuario.getContrasenaCambiadaEn());
        verify(bloqueoLoginService).limpiar("daniel@correo.com");
        verify(correoService).enviar(eq("Daniel@Correo.com"), eq("Tu contraseña de Metamorfosis cambió"),
                contains("https://metamorfosis.app/login"), contains("Hola, Daniel:"));
    }

    @Test
    void noPideLaContrasenaActual() {
        conUsuarioQueSeGuarda();
        when(jwtService.generarToken(usuario)).thenReturn("token-nuevo");

        assertDoesNotThrow(() -> servicio.cambiar(1, new CambiarContrasenaRequest(NUEVA, NUEVA)));
    }

    @Test
    void siElCorreoDeAvisoFallaLaContrasenaIgualQuedaCambiada() {
        conUsuarioQueSeGuarda();
        when(jwtService.generarToken(usuario)).thenReturn("token-nuevo");
        doThrow(new CorreoNoEnviadoException("Brevo no responde"))
                .when(correoService).enviar(anyString(), anyString(), anyString(), anyString());

        ContrasenaCambiadaResponse respuesta = servicio.cambiar(1, new CambiarContrasenaRequest(NUEVA, NUEVA));

        assertEquals("token-nuevo", respuesta.token());
        assertTrue(passwordEncoder.matches(NUEVA, usuario.getPasswordHash()));
    }

    @Test
    void laNuevaDebeSerDistintaDeLaActual() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        ValidacionException e = fallaCon(new CambiarContrasenaRequest(ACTUAL, ACTUAL));

        assertEquals(List.of(ContrasenaService.MENSAJE_IGUAL_A_LA_ACTUAL), e.getErrores());
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(correoService);
    }

    @Test
    void sinLaNuevaLaPide() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        assertEquals(List.of(ContrasenaService.MENSAJE_FALTA_NUEVA),
                fallaCon(new CambiarContrasenaRequest("", null)).getErrores());
        assertEquals(List.of(ContrasenaService.MENSAJE_FALTA_NUEVA), fallaCon(null).getErrores());
    }

    @Test
    void laNuevaDebeCumplirLaReglaYCoincidirConLaConfirmacion() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        ValidacionException e = fallaCon(new CambiarContrasenaRequest("corta", "otra"));

        assertEquals(List.of(ReglaContrasena.MENSAJE, ContrasenaService.MENSAJE_NO_COINCIDEN), e.getErrores());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void usuarioQueNoExiste() {
        when(usuarioRepository.findById(9)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> servicio.cambiar(9, new CambiarContrasenaRequest(NUEVA, NUEVA)));
    }
}
