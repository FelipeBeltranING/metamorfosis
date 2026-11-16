package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.LoginRequest;
import com.metamorfosis.backend.dto.LoginResponse;
import com.metamorfosis.backend.exception.CredencialesInvalidasException;
import com.metamorfosis.backend.exception.CuentaBloqueadaException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.ResultadoLog;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.TokenRevocadoRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** HU9: caja blanca de AuthService.iniciarSesion (plan de pruebas de la sección 16.7). */
@ExtendWith(MockitoExtension.class)
class AuthServiceIniciarSesionTest {

    private static final String EMAIL = "ana@test.com";
    private static final String CLAVE = "Clave123";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TokenRevocadoRepository tokenRevocadoRepository;
    @Mock private LogService logService;
    @Mock private BloqueoLoginService bloqueoLoginService;
    @Mock private SolicitudRegistroService solicitudRegistroService;

    private AuthService servicio;
    private Usuario ana;

    @BeforeEach
    void preparar() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash-relleno");
        servicio = new AuthService(usuarioRepository, passwordEncoder, jwtService, tokenRevocadoRepository,
                logService, bloqueoLoginService, solicitudRegistroService);
        ana = new Usuario("Ana", "Ruiz", EMAIL, "hash-de-ana");
        ana.setCodigoUsuario(3);
    }

    @Test
    void camposVaciosOEmailMalFormadoSeRechazanSinBuscarAlUsuario() {
        ValidacionException vacios = assertThrows(ValidacionException.class,
                () -> servicio.iniciarSesion(new LoginRequest(" ", "")));
        assertEquals(List.of("El email es obligatorio", "La contraseña es obligatoria"), vacios.getErrores());

        ValidacionException formato = assertThrows(ValidacionException.class,
                () -> servicio.iniciarSesion(new LoginRequest("ana@", CLAVE)));
        assertEquals(List.of("El formato del email no es válido"), formato.getErrores());
        verifyNoInteractions(usuarioRepository, jwtService);
    }

    @Test
    void elEmailSeNormalizaAntesDeBuscar() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(ana));
        when(passwordEncoder.matches(CLAVE, "hash-de-ana")).thenReturn(true);
        when(jwtService.generarToken(ana)).thenReturn("jwt");

        servicio.iniciarSesion(new LoginRequest("  ANA@Test.COM ", CLAVE));

        verify(usuarioRepository).findByEmailIgnoreCase(EMAIL);
        verify(bloqueoLoginService).limpiar(EMAIL);
    }

    @Test
    void emailBloqueadoSeRechazaSinRevisarLaContrasena() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(ana));
        when(bloqueoLoginService.segundosBloqueado(EMAIL)).thenReturn(120L);

        assertThrows(CuentaBloqueadaException.class, () -> servicio.iniciarSesion(new LoginRequest(EMAIL, CLAVE)));
        verify(passwordEncoder, never()).matches(eq(CLAVE), eq("hash-de-ana"));
        verify(jwtService, never()).generarToken(any());
        verify(logService).registrar(3, AccionLog.INICIAR_SESION, EntidadLog.USUARIO, 3, ResultadoLog.ERROR,
                "{\"motivo\":\"CUENTA_BLOQUEADA\"}");
    }

    @Test
    void emailNoRegistradoRespondeElMismoMensajeYCompararContraElHashDeRelleno() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        CredencialesInvalidasException e = assertThrows(CredencialesInvalidasException.class,
                () -> servicio.iniciarSesion(new LoginRequest(EMAIL, CLAVE)));
        assertEquals("Email o contraseña incorrectos", e.getMessage());
        verify(passwordEncoder).matches(CLAVE, "hash-relleno");
        verify(bloqueoLoginService).registrarFallo(EMAIL);
        verify(logService).registrar(null, AccionLog.INICIAR_SESION, EntidadLog.USUARIO, null, ResultadoLog.ERROR,
                "{\"motivo\":\"USUARIO_INEXISTENTE\"}");
    }

    @Test
    void contrasenaIncorrectaRespondeElMismoMensajeYSumaUnFallo() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(ana));
        when(passwordEncoder.matches("Otra1234", "hash-de-ana")).thenReturn(false);

        CredencialesInvalidasException e = assertThrows(CredencialesInvalidasException.class,
                () -> servicio.iniciarSesion(new LoginRequest(EMAIL, "Otra1234")));
        assertEquals("Email o contraseña incorrectos", e.getMessage());
        verify(bloqueoLoginService).registrarFallo(EMAIL);
        verify(jwtService, never()).generarToken(any());
        verify(logService).registrar(3, AccionLog.INICIAR_SESION, EntidadLog.USUARIO, 3, ResultadoLog.ERROR,
                "{\"motivo\":\"PASSWORD_INCORRECTO\"}");
    }

    @Test
    void credencialesCorrectasDevuelvenElTokenYLimpianLosFallos() {
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(ana));
        when(passwordEncoder.matches(CLAVE, "hash-de-ana")).thenReturn(true);
        when(jwtService.generarToken(ana)).thenReturn("jwt-de-ana");

        LoginResponse respuesta = servicio.iniciarSesion(new LoginRequest(EMAIL, CLAVE));

        assertEquals(new LoginResponse("jwt-de-ana", 3, "Ana", "Ruiz", EMAIL), respuesta);
        verify(bloqueoLoginService).limpiar(EMAIL);
        verify(bloqueoLoginService, never()).registrarFallo(any());
        verify(logService).registrar(3, AccionLog.INICIAR_SESION, EntidadLog.USUARIO, 3, ResultadoLog.EXITO, null);
    }
}
