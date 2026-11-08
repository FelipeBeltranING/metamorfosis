package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.InvitacionCorreoResponse;
import com.metamorfosis.backend.dto.InvitacionRespondidaResponse;
import com.metamorfosis.backend.dto.InvitacionResponse;
import com.metamorfosis.backend.dto.ResponderInvitacionRequest;
import com.metamorfosis.backend.exception.InvitacionNoEncontradaException;
import com.metamorfosis.backend.exception.InvitacionNoValidaException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.EstadoInvitacion;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Participacion;
import com.metamorfosis.backend.model.ParticipacionId;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.ResultadoLog;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.ParticipacionRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import com.metamorfosis.backend.service.correo.CorreoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** HU14: caja blanca de InvitacionService (ver y responder, desde la app y desde el correo). */
@ExtendWith(MockitoExtension.class)
class InvitacionServiceResponderTest {

    private static final Integer CREADOR = 1;
    private static final Integer INVITADA = 2;
    private static final Integer CODIGO_META = 10;
    private static final String TOKEN = "token-del-correo";
    private static final LocalDateTime AHORA = LocalDateTime.parse("2026-11-07T15:00:00");

    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ParticipacionRepository participacionRepository;
    @Mock private CorreoService correoService;
    @Mock private LogService logService;

    private InvitacionService servicio;
    private Meta meta;
    private Participacion invitacion;

    @BeforeEach
    void preparar() throws Exception {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-07T15:00:00Z"), ZoneOffset.UTC);
        servicio = new InvitacionService(metaRepository, usuarioRepository, participacionRepository, correoService,
                logService, "http://localhost:5173", reloj);
        meta = new Meta("Leer 20 libros", Plazo.MEDIANO, TipoMeta.PERSONAL);
        meta.crear(usuario(CREADOR, "Daniel", "Santos", "daniel@test.com"));
        Field id = Meta.class.getDeclaredField("codigoMeta");
        id.setAccessible(true);
        id.set(meta, CODIGO_META);
        invitacion = meta.invitar(usuario(INVITADA, "Laura", "Gómez", "laura.gomez@gmail.com"), CREADOR,
                TokensSeguros.hash(TOKEN), AHORA.minusDays(1), AHORA.plusDays(6));
    }

    private static Usuario usuario(Integer codigo, String nombre, String apellido, String email) {
        Usuario u = new Usuario(nombre, apellido, email, "hash");
        u.setCodigoUsuario(codigo);
        return u;
    }

    private void laInvitacionExiste() {
        when(participacionRepository.findById(new ParticipacionId(INVITADA, CODIGO_META))).thenReturn(Optional.of(invitacion));
    }

    private void elTokenExiste() {
        when(participacionRepository.findByTokenHash(TokensSeguros.hash(TOKEN))).thenReturn(Optional.of(invitacion));
    }

    @Test
    void listaLasPendientesConQuienInvitoYCuantosParticipan() {
        when(participacionRepository.pendientesDe(INVITADA)).thenReturn(List.of(invitacion));

        List<InvitacionResponse> lista = servicio.pendientes(INVITADA);

        assertEquals(1, lista.size());
        InvitacionResponse i = lista.get(0);
        assertEquals("Leer 20 libros", i.nombreMeta());
        assertEquals("Daniel", i.invitador().nombre());
        assertEquals(1, i.participantes());
    }

    @Test
    void aceptarDesdeLaAppLaDejaParticipandoYAnulaElEnlace() {
        laInvitacionExiste();

        InvitacionRespondidaResponse r = servicio.responder(CODIGO_META, new ResponderInvitacionRequest(true), INVITADA);

        assertTrue(r.aceptada());
        assertEquals(EstadoInvitacion.ACEPTADA, invitacion.getEstadoInvitacion());
        assertNull(invitacion.getTokenExpira());
        assertTrue(meta.participaAceptado(INVITADA));
        verify(participacionRepository).save(invitacion);
    }

    @Test
    void rechazarDesdeLaApp() {
        laInvitacionExiste();

        InvitacionRespondidaResponse r = servicio.responder(CODIGO_META, new ResponderInvitacionRequest(false), INVITADA);

        assertFalse(r.aceptada());
        assertEquals(EstadoInvitacion.RECHAZADA, invitacion.getEstadoInvitacion());
        assertEquals(1, meta.participantesVisibles().size());
    }

    @Test
    void sinInvitacionPendiente() {
        when(participacionRepository.findById(new ParticipacionId(3, CODIGO_META))).thenReturn(Optional.empty());

        assertThrows(InvitacionNoEncontradaException.class,
                () -> servicio.responder(CODIGO_META, new ResponderInvitacionRequest(true), 3));
    }

    @Test
    void unaInvitacionYaRespondidaNoSeVuelveAResponder() {
        invitacion.responder(false);
        laInvitacionExiste();

        assertThrows(InvitacionNoEncontradaException.class,
                () -> servicio.responder(CODIGO_META, new ResponderInvitacionRequest(true), INVITADA));
    }

    @Test
    void respuestaVaciaEsInvalida() {
        assertThrows(ValidacionException.class,
                () -> servicio.responder(CODIGO_META, new ResponderInvitacionRequest(null), INVITADA));
    }

    @Test
    void noSeAceptaUnaMetaYaCumplidaPeroSiSePuedeRechazar() {
        meta.marcarCumplida(CREADOR, LocalDate.of(2026, 11, 6));
        laInvitacionExiste();

        assertThrows(ValidacionException.class,
                () -> servicio.responder(CODIGO_META, new ResponderInvitacionRequest(true), INVITADA));
        assertEquals(EstadoInvitacion.PENDIENTE, invitacion.getEstadoInvitacion());

        servicio.responder(CODIGO_META, new ResponderInvitacionRequest(false), INVITADA);
        assertEquals(EstadoInvitacion.RECHAZADA, invitacion.getEstadoInvitacion());
    }

    @Test
    void laPaginaDelCorreoMuestraLaMetaYElCorreoEnmascarado() {
        elTokenExiste();

        InvitacionCorreoResponse r = servicio.consultarPorToken(TOKEN);

        assertEquals("Leer 20 libros", r.nombreMeta());
        assertEquals("Daniel Santos", r.nombreInvitador());
        assertEquals("l***@gmail.com", r.paraEmail());
    }

    @Test
    void enlaceVencidoOInexistente() {
        when(participacionRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
        assertThrows(InvitacionNoValidaException.class, () -> servicio.consultarPorToken("otro"));
        assertThrows(InvitacionNoValidaException.class, () -> servicio.consultarPorToken(" "));

        invitacion.invitar(CREADOR, TokensSeguros.hash(TOKEN), AHORA.minusDays(8), AHORA.minusDays(1));
        elTokenExiste();
        assertThrows(InvitacionNoValidaException.class, () -> servicio.consultarPorToken(TOKEN));
    }

    @Test
    void aceptarDesdeElCorreoConLaCuentaInvitada() {
        elTokenExiste();

        InvitacionRespondidaResponse r = servicio.aceptarPorToken(TOKEN, INVITADA);

        assertTrue(r.aceptada());
        assertEquals(EstadoInvitacion.ACEPTADA, invitacion.getEstadoInvitacion());
    }

    @Test
    void otraCuentaNoPuedeAceptarConElEnlace() {
        elTokenExiste();

        PermisoDenegadoException e = assertThrows(PermisoDenegadoException.class,
                () -> servicio.aceptarPorToken(TOKEN, 3));
        assertTrue(e.getMessage().contains("l***@gmail.com"));
        assertEquals(EstadoInvitacion.PENDIENTE, invitacion.getEstadoInvitacion());
    }

    @Test
    void rechazarDesdeElCorreoSinSesionQuedaEnElLog() {
        elTokenExiste();

        servicio.rechazarPorToken(TOKEN);

        assertEquals(EstadoInvitacion.RECHAZADA, invitacion.getEstadoInvitacion());
        verify(logService).registrar(eq(INVITADA), eq(AccionLog.RESPONDER_INVITACION), eq(EntidadLog.META),
                eq(CODIGO_META), eq(ResultadoLog.EXITO), any());
    }

    @Test
    void elEnlaceSirveUnaSolaVez() {
        elTokenExiste();
        servicio.aceptarPorToken(TOKEN, INVITADA);

        assertThrows(InvitacionNoValidaException.class, () -> servicio.consultarPorToken(TOKEN));
    }

    @Test
    void enmascarar() {
        assertEquals("l***@gmail.com", InvitacionService.enmascarar("laura@gmail.com"));
        assertEquals("***", InvitacionService.enmascarar("sin-arroba"));
    }
}
