package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.EstadoSolicitudResponse;
import com.metamorfosis.backend.dto.SolicitudRegistroRequest;
import com.metamorfosis.backend.dto.SolicitudRegistroResponse;
import com.metamorfosis.backend.exception.EmailYaRegistradoException;
import com.metamorfosis.backend.exception.SolicitudBloqueadaException;
import com.metamorfosis.backend.exception.SolicitudInvalidaException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.ResultadoLog;
import com.metamorfosis.backend.model.SolicitudRegistro;
import com.metamorfosis.backend.repository.SolicitudRegistroRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import com.metamorfosis.backend.service.correo.CorreoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * HU8: caja blanca de SolicitudRegistroService, los pasos previos a crear la cuenta: pedir el correo
 * de autorización, autorizar, consultar el estado y "No fui yo" (plan de pruebas de la sección 16.6).
 */
@ExtendWith(MockitoExtension.class)
class SolicitudRegistroServiceTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 15, 0);
    private static final String EMAIL = "ana@test.com";
    private static final String TOKEN = "token-del-correo";

    @Mock private SolicitudRegistroRepository solicitudRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CorreoService correoService;
    @Mock private LogService logService;

    private SolicitudRegistroService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        servicio = new SolicitudRegistroService(solicitudRepository, usuarioRepository, correoService, logService,
                "http://localhost:5173/", 30, reloj);
    }

    /** Solicitud vigente con el token del correo, autorizada o no. */
    private SolicitudRegistro vigente(boolean autorizada) {
        SolicitudRegistro s = new SolicitudRegistro(EMAIL);
        s.renovar(SolicitudRegistroService.hash(TOKEN), SolicitudRegistroService.hash("sesion"),
                AHORA.minusMinutes(5), AHORA.plusMinutes(25));
        if (autorizada) s.autorizar(AHORA.minusMinutes(1));
        return s;
    }

    @Test
    void emailVacioOMalFormadoSeRechazaSinEnviarCorreo() {
        ValidacionException vacio = assertThrows(ValidacionException.class,
                () -> servicio.solicitar(new SolicitudRegistroRequest("  ")));
        assertEquals(List.of("El email es obligatorio"), vacio.getErrores());

        ValidacionException formato = assertThrows(ValidacionException.class,
                () -> servicio.solicitar(new SolicitudRegistroRequest("juan@")));
        assertEquals(List.of("El formato del email no es válido"), formato.getErrores());
        verifyNoInteractions(correoService);
    }

    @Test
    void emailYaRegistradoSinDistinguirMayusculasResponde409() {
        when(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        assertThrows(EmailYaRegistradoException.class,
                () -> servicio.solicitar(new SolicitudRegistroRequest("  ANA@Test.com ")));
        verifyNoInteractions(correoService);
    }

    @Test
    void solicitudValidaGuardaSoloElHashYEnviaElCorreo() {
        when(solicitudRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        SolicitudRegistroResponse respuesta = servicio.solicitar(new SolicitudRegistroRequest(EMAIL));

        ArgumentCaptor<SolicitudRegistro> guardada = ArgumentCaptor.forClass(SolicitudRegistro.class);
        verify(solicitudRepository).save(guardada.capture());
        assertEquals(64, guardada.getValue().getTokenHash().length(), "en la base queda el SHA-256, no el token");
        assertEquals(AHORA.plusMinutes(30), guardada.getValue().getFechaExpiracion());
        assertFalse(guardada.getValue().estaAutorizada());
        verify(correoService).enviar(eq(EMAIL), anyString(), anyString(), anyString());
        verify(logService).registrar(null, AccionLog.SOLICITAR_REGISTRO, EntidadLog.USUARIO, null, ResultadoLog.EXITO, null);
        assertEquals(EMAIL, respuesta.email());
        assertNotNull(respuesta.sesion());
    }

    @Test
    void pedirOtroCorreoAntesDeUnMinutoResponde429() {
        SolicitudRegistro reciente = new SolicitudRegistro(EMAIL);
        reciente.renovar("h", "s", AHORA.minusSeconds(30), AHORA.plusMinutes(29));
        when(solicitudRepository.findByEmail(EMAIL)).thenReturn(Optional.of(reciente));

        assertThrows(SolicitudBloqueadaException.class, () -> servicio.solicitar(new SolicitudRegistroRequest(EMAIL)));
        verifyNoInteractions(correoService);
    }

    @Test
    void emailBloqueadoPorNoFuiYoResponde429() {
        SolicitudRegistro bloqueada = vigente(false);
        bloqueada.bloquear(AHORA.plusHours(20));
        when(solicitudRepository.findByEmail(EMAIL)).thenReturn(Optional.of(bloqueada));

        SolicitudBloqueadaException e = assertThrows(SolicitudBloqueadaException.class,
                () -> servicio.solicitar(new SolicitudRegistroRequest(EMAIL)));
        assertTrue(e.getMessage().startsWith("El dueño de este correo rechazó el registro"));
    }

    @Test
    void autorizarMarcaLaSolicitudYElEstadoPasaAAutorizada() {
        SolicitudRegistro s = vigente(false);
        when(solicitudRepository.findByTokenHash(SolicitudRegistroService.hash(TOKEN))).thenReturn(Optional.of(s));
        when(solicitudRepository.findBySesionHash(SolicitudRegistroService.hash("sesion"))).thenReturn(Optional.of(s));

        assertEquals(EstadoSolicitudResponse.Estado.PENDIENTE, servicio.estado("sesion").estado());
        servicio.autorizar(TOKEN);
        assertTrue(s.estaAutorizada());
        assertEquals(EstadoSolicitudResponse.Estado.AUTORIZADA, servicio.estado("sesion").estado());
    }

    @Test
    void validarParaRegistroExigeEnlaceVigenteYAutorizado() {
        assertThrows(SolicitudInvalidaException.class, () -> servicio.validarParaRegistro(null));

        when(solicitudRepository.findByTokenHash(SolicitudRegistroService.hash(TOKEN))).thenReturn(Optional.of(vigente(false)));
        SolicitudInvalidaException sinAutorizar = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.validarParaRegistro(TOKEN));
        assertEquals("Primero autoriza el registro desde el correo que te enviamos.", sinAutorizar.getMessage());

        SolicitudRegistro autorizada = vigente(true);
        when(solicitudRepository.findByTokenHash(SolicitudRegistroService.hash(TOKEN))).thenReturn(Optional.of(autorizada));
        assertSame(autorizada, servicio.validarParaRegistro(TOKEN));
    }

    @Test
    void enlaceVencidoNoSirve() {
        SolicitudRegistro vencida = new SolicitudRegistro(EMAIL);
        vencida.renovar(SolicitudRegistroService.hash(TOKEN), "s", AHORA.minusMinutes(40), AHORA.minusMinutes(10));
        vencida.autorizar(AHORA.minusMinutes(35));
        when(solicitudRepository.findByTokenHash(SolicitudRegistroService.hash(TOKEN))).thenReturn(Optional.of(vencida));

        assertThrows(SolicitudInvalidaException.class, () -> servicio.validarParaRegistro(TOKEN));
    }

    @Test
    void noFuiYoInvalidaElEnlaceYBloqueaElEmail24Horas() {
        SolicitudRegistro s = vigente(false);
        when(solicitudRepository.findByTokenHash(SolicitudRegistroService.hash(TOKEN))).thenReturn(Optional.of(s));

        servicio.rechazar(TOKEN);

        assertNull(s.getTokenHash());
        assertEquals(AHORA.plusHours(24), s.getBloqueadoHasta());
        verify(solicitudRepository).save(s);
        verify(logService).registrar(null, AccionLog.RECHAZAR_REGISTRO, EntidadLog.USUARIO, null, ResultadoLog.EXITO, null);
    }
}
