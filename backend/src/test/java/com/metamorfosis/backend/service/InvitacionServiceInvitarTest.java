package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.InvitacionEnviadaResponse;
import com.metamorfosis.backend.dto.InvitarRequest;
import com.metamorfosis.backend.exception.ConflictoException;
import com.metamorfosis.backend.exception.CorreoNoEnviadoException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.EstadoInvitacion;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Participacion;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.ParticipacionRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import com.metamorfosis.backend.service.correo.CorreoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** HU13: caja blanca de InvitacionService.invitar. */
@ExtendWith(MockitoExtension.class)
class InvitacionServiceInvitarTest {

    private static final Integer CREADOR = 1;
    private static final Integer INVITADO = 2;
    private static final Integer CODIGO_META = 10;

    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ParticipacionRepository participacionRepository;
    @Mock private CorreoService correoService;
    @Mock private LogService logService;

    private InvitacionService servicio;
    private Meta meta;
    private Usuario invitado;

    @BeforeEach
    void preparar() throws Exception {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-07T15:00:00Z"), ZoneOffset.UTC);
        servicio = new InvitacionService(metaRepository, usuarioRepository, participacionRepository, correoService,
                logService, "http://localhost:5173", reloj);
        meta = new Meta("Leer 20 libros", Plazo.MEDIANO, TipoMeta.PERSONAL);
        meta.crear(usuario(CREADOR, "Daniel", "Santos"));
        Field id = Meta.class.getDeclaredField("codigoMeta");
        id.setAccessible(true);
        id.set(meta, CODIGO_META);
        invitado = usuario(INVITADO, "Laura", "Gómez");
    }

    private static Usuario usuario(Integer codigo, String nombre, String apellido) {
        Usuario u = new Usuario(nombre, apellido, nombre.toLowerCase() + "@test.com", "hash");
        u.setCodigoUsuario(codigo);
        return u;
    }

    private void metaEInvitadoExisten() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(usuarioRepository.findById(INVITADO)).thenReturn(Optional.of(invitado));
    }

    private Participacion participacionDe(Integer codigo) {
        return meta.participacionDeUsuario(codigo).orElseThrow();
    }

    @Test
    void invitaQuedaPendienteYEnviaElCorreoConLosDosEnlaces() {
        metaEInvitadoExisten();

        InvitacionEnviadaResponse r = servicio.invitar(CODIGO_META, new InvitarRequest(INVITADO), CREADOR);

        assertTrue(r.correoEnviado());
        assertEquals(EstadoInvitacion.PENDIENTE, r.participante().estado());
        assertFalse(r.participante().creador());
        Participacion p = participacionDe(INVITADO);
        assertEquals(CREADOR, p.getCodigoInvitador());
        assertEquals(java.time.LocalDateTime.parse("2026-11-14T15:00:00"), p.getTokenExpira());
        verify(metaRepository).save(meta);

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviar(eq("laura@test.com"), eq("Daniel Santos te invitó a una meta en Metamorfosis"),
                html.capture(), anyString());
        assertTrue(html.getValue().contains("Leer 20 libros"));
        assertTrue(html.getValue().contains("/invitaciones/responder?token="));
        assertTrue(html.getValue().contains("accion=aceptar"));
        assertTrue(html.getValue().contains("accion=rechazar"));
    }

    @Test
    void siElCorreoFallaLaInvitacionQuedaIgual() {
        metaEInvitadoExisten();
        doThrow(new CorreoNoEnviadoException("Brevo caído")).when(correoService).enviar(any(), any(), any(), any());

        InvitacionEnviadaResponse r = servicio.invitar(CODIGO_META, new InvitarRequest(INVITADO), CREADOR);

        assertFalse(r.correoEnviado());
        assertEquals(EstadoInvitacion.PENDIENTE, participacionDe(INVITADO).getEstadoInvitacion());
        verify(metaRepository).save(meta);
    }

    @Test
    void soloElCreadorInvita() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        assertThrows(PermisoDenegadoException.class,
                () -> servicio.invitar(CODIGO_META, new InvitarRequest(3), INVITADO));
        verifyNoInteractions(correoService);
    }

    @Test
    void noSeInvitaAUnaMetaCumplida() {
        meta.marcarCumplida(CREADOR, java.time.LocalDate.of(2026, 11, 1));
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> servicio.invitar(CODIGO_META, new InvitarRequest(INVITADO), CREADOR));
        assertEquals("Solo se puede invitar a metas activas", e.getErrores().get(0));
    }

    @Test
    void noPuedesInvitarteATiMismo() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        assertThrows(ValidacionException.class,
                () -> servicio.invitar(CODIGO_META, new InvitarRequest(CREADOR), CREADOR));
    }

    @Test
    void pedidoSinPersonaEsInvalido() {
        assertThrows(ValidacionException.class, () -> servicio.invitar(CODIGO_META, new InvitarRequest(null), CREADOR));
        assertThrows(ValidacionException.class, () -> servicio.invitar(CODIGO_META, null, CREADOR));
    }

    @Test
    void personaInexistente() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> servicio.invitar(CODIGO_META, new InvitarRequest(99), CREADOR));
    }

    @Test
    void noSeInvitaDosVeces() {
        metaEInvitadoExisten();
        servicio.invitar(CODIGO_META, new InvitarRequest(INVITADO), CREADOR);

        ConflictoException e = assertThrows(ConflictoException.class,
                () -> servicio.invitar(CODIGO_META, new InvitarRequest(INVITADO), CREADOR));
        assertEquals("Laura Gómez ya tiene una invitación pendiente a esta meta", e.getMessage());
    }

    @Test
    void noSeInvitaAQuienYaParticipa() {
        metaEInvitadoExisten();
        servicio.invitar(CODIGO_META, new InvitarRequest(INVITADO), CREADOR);
        participacionDe(INVITADO).responder(true);

        assertThrows(ConflictoException.class,
                () -> servicio.invitar(CODIGO_META, new InvitarRequest(INVITADO), CREADOR));
    }

    @Test
    void aQuienRechazoSeLePuedeVolverAInvitar() {
        metaEInvitadoExisten();
        servicio.invitar(CODIGO_META, new InvitarRequest(INVITADO), CREADOR);
        participacionDe(INVITADO).responder(false);

        InvitacionEnviadaResponse r = servicio.invitar(CODIGO_META, new InvitarRequest(INVITADO), CREADOR);

        assertEquals(EstadoInvitacion.PENDIENTE, r.participante().estado());
        assertEquals(2, meta.participantesVisibles().size());
        verify(correoService, times(2)).enviar(any(), any(), any(), any());
    }

    @Test
    void maximoDePersonasPorMeta() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        for (int i = 0; i < InvitacionService.MAXIMO_PARTICIPANTES - 1; i++) {
            Usuario u = usuario(100 + i, "Persona" + i, "Prueba");
            meta.invitar(u, CREADOR, "h" + i, java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        }
        when(usuarioRepository.findById(INVITADO)).thenReturn(Optional.of(invitado));

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> servicio.invitar(CODIGO_META, new InvitarRequest(INVITADO), CREADOR));
        assertEquals("La meta ya tiene el máximo de 10 personas", e.getErrores().get(0));
    }
}
