package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.CodigoUnionResponse;
import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.dto.UnirseRequest;
import com.metamorfosis.backend.exception.ConflictoException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.SolicitudBloqueadaException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.EstadoInvitacion;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Caja blanca de CodigoUnionService (generar, desactivar y unirse con código). */
@ExtendWith(MockitoExtension.class)
class CodigoUnionServiceTest {

    private static final Integer CREADOR = 1;
    private static final Integer PERSONA = 2;
    private static final Integer CODIGO_META = 10;
    private static final LocalDateTime AHORA = LocalDateTime.parse("2026-11-07T15:00:00");

    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private CodigoUnionService servicio;
    private Meta meta;
    private Usuario persona;

    @BeforeEach
    void preparar() throws Exception {
        servicio = new CodigoUnionService(metaRepository, usuarioRepository,
                Clock.fixed(Instant.parse("2026-11-07T15:00:00Z"), ZoneOffset.UTC));
        meta = new Meta("Leer 20 libros", Plazo.MEDIANO, TipoMeta.PERSONAL);
        meta.crear(usuario(CREADOR));
        Field id = Meta.class.getDeclaredField("codigoMeta");
        id.setAccessible(true);
        id.set(meta, CODIGO_META);
        persona = usuario(PERSONA);
    }

    private static Usuario usuario(Integer codigo) {
        Usuario u = new Usuario("Usuario" + codigo, "Prueba", "u" + codigo + "@test.com", "hash");
        u.setCodigoUsuario(codigo);
        return u;
    }

    private void conCodigo(String codigo) {
        meta.activarCodigoUnion(codigo, AHORA.plusDays(7));
        when(metaRepository.findByCodigoUnion(codigo)).thenReturn(Optional.of(meta));
    }

    @Test
    void generaUnCodigoDeOchoCaracteresQueVenceEnSieteDias() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(metaRepository.findByCodigoUnion(anyString())).thenReturn(Optional.empty());

        CodigoUnionResponse r = servicio.generar(CODIGO_META, CREADOR);

        assertEquals(8, r.codigo().length());
        assertTrue(r.codigo().chars().allMatch(c -> CodigoUnionService.ALFABETO.indexOf(c) >= 0));
        assertEquals(AHORA.plusDays(7), r.expira());
        verify(metaRepository).save(meta);
    }

    @Test
    void siElCodigoYaExisteGeneraOtro() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(metaRepository.findByCodigoUnion(anyString()))
                .thenReturn(Optional.of(new Meta("Otra", Plazo.CORTO, TipoMeta.OTRO)))
                .thenReturn(Optional.empty());

        servicio.generar(CODIGO_META, CREADOR);

        verify(metaRepository, times(2)).findByCodigoUnion(anyString());
    }

    @Test
    void soloElCreadorManejaElCodigo() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        assertThrows(PermisoDenegadoException.class, () -> servicio.generar(CODIGO_META, PERSONA));
        assertThrows(PermisoDenegadoException.class, () -> servicio.verCodigo(CODIGO_META, PERSONA));
        assertThrows(PermisoDenegadoException.class, () -> servicio.desactivar(CODIGO_META, PERSONA));
    }

    @Test
    void noSeGeneraCodigoParaUnaMetaCumplida() {
        meta.marcarCumplida(CREADOR, LocalDate.of(2026, 11, 6));
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        assertThrows(ValidacionException.class, () -> servicio.generar(CODIGO_META, CREADOR));
    }

    @Test
    void verYDesactivar() {
        meta.activarCodigoUnion("ABCDEFGH", AHORA.plusDays(3));
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        assertEquals("ABCDEFGH", servicio.verCodigo(CODIGO_META, CREADOR).codigo());
        servicio.desactivar(CODIGO_META, CREADOR);
        assertNull(servicio.verCodigo(CODIGO_META, CREADOR).codigo());
    }

    @Test
    void unCodigoVencidoNoSeMuestra() {
        meta.activarCodigoUnion("ABCDEFGH", AHORA.minusMinutes(1));
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        assertEquals(CodigoUnionResponse.NINGUNO, servicio.verCodigo(CODIGO_META, CREADOR));
    }

    @Test
    void seUneConElCodigoEscritoComoSea() {
        conCodigo("ABCDEFGH");
        when(usuarioRepository.findById(PERSONA)).thenReturn(Optional.of(persona));
        when(metaRepository.save(meta)).thenReturn(meta);

        MetaResponse r = servicio.unirse(new UnirseRequest(" abcd-efgh "), PERSONA);

        assertEquals(CODIGO_META, r.codigoMeta());
        assertTrue(meta.participaAceptado(PERSONA));
        assertEquals(2, r.participantes().size());
    }

    @Test
    void siTeniaUnaInvitacionPendienteLaAcepta() {
        meta.invitar(persona, CREADOR, "hash", AHORA, AHORA.plusDays(7));
        conCodigo("ABCDEFGH");
        when(usuarioRepository.findById(PERSONA)).thenReturn(Optional.of(persona));
        when(metaRepository.save(meta)).thenReturn(meta);

        servicio.unirse(new UnirseRequest("ABCDEFGH"), PERSONA);

        assertEquals(EstadoInvitacion.ACEPTADA, meta.participacionDeUsuario(PERSONA).orElseThrow().getEstadoInvitacion());
        assertEquals(2, meta.participantesVisibles().size());
    }

    @Test
    void quienYaParticipaNoSeUneDeNuevo() {
        conCodigo("ABCDEFGH");

        assertThrows(ConflictoException.class, () -> servicio.unirse(new UnirseRequest("ABCDEFGH"), CREADOR));
    }

    @Test
    void codigoInexistenteVencidoOVacio() {
        when(metaRepository.findByCodigoUnion("ZZZZZZZZ")).thenReturn(Optional.empty());
        assertThrows(ValidacionException.class, () -> servicio.unirse(new UnirseRequest("ZZZZ-ZZZZ"), PERSONA));
        assertThrows(ValidacionException.class, () -> servicio.unirse(new UnirseRequest("corto"), PERSONA));
        assertThrows(ValidacionException.class, () -> servicio.unirse(new UnirseRequest(""), PERSONA));

        meta.activarCodigoUnion("ABCDEFGH", AHORA.minusDays(1));
        when(metaRepository.findByCodigoUnion("ABCDEFGH")).thenReturn(Optional.of(meta));
        assertThrows(ValidacionException.class, () -> servicio.unirse(new UnirseRequest("ABCDEFGH"), PERSONA));
    }

    @Test
    void conDiezFallosSeBloqueaUnRato() {
        when(metaRepository.findByCodigoUnion("ZZZZZZZZ")).thenReturn(Optional.empty());
        for (int i = 0; i < CodigoUnionService.MAXIMO_FALLOS; i++) {
            assertThrows(ValidacionException.class, () -> servicio.unirse(new UnirseRequest("ZZZZZZZZ"), PERSONA));
        }

        assertThrows(SolicitudBloqueadaException.class, () -> servicio.unirse(new UnirseRequest("ABCDEFGH"), PERSONA));
        verify(metaRepository, times(CodigoUnionService.MAXIMO_FALLOS)).findByCodigoUnion(anyString());
    }

    @Test
    void maximoDePersonas() {
        for (int i = 0; i < InvitacionService.MAXIMO_PARTICIPANTES - 1; i++) {
            meta.invitar(usuario(100 + i), CREADOR, "h" + i, AHORA, AHORA.plusDays(7));
        }
        conCodigo("ABCDEFGH");

        assertThrows(ValidacionException.class, () -> servicio.unirse(new UnirseRequest("ABCDEFGH"), PERSONA));
    }

    @Test
    void normalizar() {
        assertEquals("ABCDEFGH", CodigoUnionService.normalizar(" abcd-efgh "));
        assertEquals("", CodigoUnionService.normalizar(null));
    }
}
