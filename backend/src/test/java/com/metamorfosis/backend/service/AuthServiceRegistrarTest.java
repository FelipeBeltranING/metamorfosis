package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.RegistroRequest;
import com.metamorfosis.backend.dto.UsuarioResponse;
import com.metamorfosis.backend.exception.EmailYaRegistradoException;
import com.metamorfosis.backend.exception.SolicitudInvalidaException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.ResultadoLog;
import com.metamorfosis.backend.model.SolicitudRegistro;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.TokenRevocadoRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * HU8: caja blanca de AuthService.registrar, el último paso del registro. El email sale de la solicitud
 * que el dueño del correo autorizó, no del formulario (plan de pruebas de la sección 16.6).
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceRegistrarTest {

    private static final String TOKEN = "token-del-correo";
    private static final String EMAIL = "ana@test.com";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TokenRevocadoRepository tokenRevocadoRepository;
    @Mock private LogService logService;
    @Mock private BloqueoLoginService bloqueoLoginService;
    @Mock private SolicitudRegistroService solicitudRegistroService;

    private AuthService servicio;
    private SolicitudRegistro autorizada;

    @BeforeEach
    void preparar() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash-relleno");
        servicio = new AuthService(usuarioRepository, passwordEncoder, jwtService, tokenRevocadoRepository,
                logService, bloqueoLoginService, solicitudRegistroService);
        autorizada = new SolicitudRegistro(EMAIL);
    }

    private RegistroRequest solicitud(String nombre, String apellido, String password) {
        return new RegistroRequest(TOKEN, nombre, apellido, password);
    }

    @Test
    void solicitudNulaSeRechazaSinConsultarNada() {
        ValidacionException e = assertThrows(ValidacionException.class, () -> servicio.registrar(null));
        assertEquals(List.of("Faltan los datos del registro"), e.getErrores());
        verifyNoInteractions(solicitudRegistroService, usuarioRepository);
    }

    @Test
    void sinAutorizacionDelCorreoNoHayRegistro() {
        when(solicitudRegistroService.validarParaRegistro(TOKEN))
                .thenThrow(new SolicitudInvalidaException("Este enlace ya no es válido. Pide uno nuevo para registrarte."));

        assertThrows(SolicitudInvalidaException.class, () -> servicio.registrar(solicitud("Ana", "Ruiz", "Clave123")));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void camposVaciosSeSenalanUnoPorUno() {
        when(solicitudRegistroService.validarParaRegistro(TOKEN)).thenReturn(autorizada);

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> servicio.registrar(solicitud(" ", null, "")));
        assertEquals(List.of("El nombre es obligatorio", "El apellido es obligatorio", "La contraseña es obligatoria"),
                e.getErrores());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void contrasenaDebilSeRechazaConLaRegla() {
        when(solicitudRegistroService.validarParaRegistro(TOKEN)).thenReturn(autorizada);

        for (String debil : List.of("Clave12", "clave123", "CLAVE123", "ClaveSinNumero")) {
            ValidacionException e = assertThrows(ValidacionException.class,
                    () -> servicio.registrar(solicitud("Ana", "Ruiz", debil)), debil);
            assertEquals(List.of(ReglaContrasena.MENSAJE), e.getErrores());
        }
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void contrasenaDeOchoCaracteresConLaReglaSeAcepta() {
        assertTrue(ReglaContrasena.cumple("Clave123"));
        assertFalse(ReglaContrasena.cumple("Clave12"));
        assertFalse(ReglaContrasena.cumple(null));
    }

    @Test
    void emailRegistradoMientrasTantoResponde409() {
        when(solicitudRegistroService.validarParaRegistro(TOKEN)).thenReturn(autorizada);
        when(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        EmailYaRegistradoException e = assertThrows(EmailYaRegistradoException.class,
                () -> servicio.registrar(solicitud("Ana", "Ruiz", "Clave123")));
        assertEquals("El email ya está registrado", e.getMessage());
        verify(usuarioRepository, never()).save(any());
        verify(solicitudRegistroService, never()).consumir(any());
    }

    @Test
    void rutaFelizGuardaElHashUsaElEmailAutorizadoYConsumeElEnlace() {
        when(solicitudRegistroService.validarParaRegistro(TOKEN)).thenReturn(autorizada);
        when(passwordEncoder.encode("Clave123")).thenReturn("$2a$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            u.setCodigoUsuario(5);
            return u;
        });

        UsuarioResponse respuesta = servicio.registrar(solicitud("  Ana ", " Ruiz ", "Clave123"));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertEquals("Ana", guardado.getValue().getNombre());
        assertEquals("Ruiz", guardado.getValue().getApellido());
        assertEquals(EMAIL, guardado.getValue().getEmail());
        assertEquals("$2a$hash", guardado.getValue().getPasswordHash());
        assertNotEquals("Clave123", guardado.getValue().getPasswordHash());
        verify(solicitudRegistroService).consumir(autorizada);
        verify(logService).registrar(5, AccionLog.REGISTRAR_USUARIO, EntidadLog.USUARIO, 5, ResultadoLog.EXITO, null);
        assertEquals(new UsuarioResponse(5, "Ana", "Ruiz", EMAIL), respuesta);
    }

    @Test
    void laRespuestaNuncaTraeLaContrasena() {
        assertEquals(List.of("codigoUsuario", "nombre", "apellido", "email"),
                java.util.Arrays.stream(UsuarioResponse.class.getRecordComponents()).map(c -> c.getName()).toList());
        verify(logService, never()).registrar(any(), any(), any(), any(), any(), isNull());
    }
}
