package com.metamorfosis.backend.service;

import com.metamorfosis.backend.config.CalendarioUsuario;
import com.metamorfosis.backend.dto.LogroHitoResponse;
import com.metamorfosis.backend.dto.SubMetaRequest;
import com.metamorfosis.backend.dto.SubMetaResponse;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.SubMeta;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.model.TipoObjetivo;
import com.metamorfosis.backend.model.TipoSubMeta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.SubMetaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HU28 (agregar hitos al camino), HU5 (marcar hitos) y HU29 (avance): caja blanca de SubMetaService. */
@ExtendWith(MockitoExtension.class)
class SubMetaServiceHitosTest {

    private static final Integer CREADOR = 1;
    private static final Integer COMPANERO = 2;
    private static final Integer AJENO = 3;
    private static final Integer CODIGO_META = 10;
    private static final LocalDate HOY = LocalDate.of(2026, 11, 14);

    @Mock private MetaRepository metaRepository;
    @Mock private SubMetaRepository subMetaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private SubMetaService servicio;
    private Meta meta;
    private Usuario companero;

    @BeforeEach
    void setUp() throws Exception {
        CalendarioUsuario calendario = new CalendarioUsuario(
                Clock.fixed(HOY.atStartOfDay(ZoneId.of("America/Bogota")).toInstant(), ZoneId.of("America/Bogota")));
        servicio = new SubMetaService(metaRepository, subMetaRepository, usuarioRepository, calendario);

        Usuario creador = new Usuario("Felipe", "Beltrán", "felipe@test.com", "hash");
        creador.setCodigoUsuario(CREADOR);
        companero = new Usuario("Daniel", "Santos", "daniel@test.com", "hash");
        companero.setCodigoUsuario(COMPANERO);

        meta = new Meta("Leer 20 libros", Plazo.MEDIANO, TipoMeta.PERSONAL);
        meta.crear(creador);
        Field id = Meta.class.getDeclaredField("codigoMeta");
        id.setAccessible(true);
        id.set(meta, CODIGO_META);
    }

    private SubMeta hito(int codigo, String nombre) throws Exception {
        SubMeta h = new SubMeta(meta, TipoSubMeta.HITO, HOY.minusDays(5));
        h.actualizar(nombre, null, null, null, null, 0, null);
        Field id = SubMeta.class.getDeclaredField("codigoSubMeta");
        id.setAccessible(true);
        id.set(h, codigo);
        return h;
    }

    private SubMeta habito(int codigo) throws Exception {
        SubMeta h = new SubMeta(meta, TipoSubMeta.SI_NO, HOY.minusDays(5));
        h.actualizar("Leer antes de dormir", null, null, null, null, SubMeta.TODOS_LOS_DIAS, null);
        Field id = SubMeta.class.getDeclaredField("codigoSubMeta");
        id.setAccessible(true);
        id.set(h, codigo);
        return h;
    }

    // ---------- HU28: agregar un hito ----------

    @Test
    void crearHitoIgnoraLosCamposDeLosHabitos() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(subMetaRepository.save(any(SubMeta.class))).thenAnswer(i -> i.getArgument(0));
        SubMetaRequest pedido = new SubMetaRequest("Terminar el libro 1", "¿Leíste?", TipoSubMeta.HITO,
                "págs", new BigDecimal("15"), TipoObjetivo.AL_MENOS, List.of(1, 3), null);

        SubMetaResponse r = servicio.crear(CODIGO_META, pedido, CREADOR);

        assertEquals(TipoSubMeta.HITO, r.tipo());
        assertNull(r.pregunta());
        assertNull(r.unidad());
        assertNull(r.objetivo());
        assertNull(r.tipoObjetivo());
        assertEquals(List.of(), r.dias());
        assertNull(r.fechaLogrado());
    }

    @Test
    void crearSinTipoPideElegirHitoOHabito() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        SubMetaRequest pedido = new SubMetaRequest("Algo", null, null, null, null, null, null, null);

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> servicio.crear(CODIGO_META, pedido, CREADOR));
        assertEquals(List.of("Elige si el paso es un hito o un hábito"), e.getErrores());
    }

    @Test
    void soloElCreadorAgregaPasos() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        SubMetaRequest pedido = new SubMetaRequest("Terminar el libro 1", null, TipoSubMeta.HITO,
                null, null, null, null, null);

        assertThrows(PermisoDenegadoException.class, () -> servicio.crear(CODIGO_META, pedido, COMPANERO));
        verify(subMetaRepository, never()).save(any());
    }

    // ---------- HU5: marcar un hito ----------

    @Test
    void unParticipanteLograElHitoHoyYQuedaQuienLoLogro() throws Exception {
        SubMeta h1 = hito(100, "Terminar el libro 1");
        SubMeta h2 = hito(101, "Terminar el libro 2");
        when(subMetaRepository.findById(100)).thenReturn(Optional.of(h1));
        when(metaRepository.esParticipanteAceptado(CODIGO_META, COMPANERO)).thenReturn(true);
        when(usuarioRepository.getReferenceById(COMPANERO)).thenReturn(companero);
        when(subMetaRepository.findByMetaCodigoMetaOrderByCodigoSubMetaAsc(CODIGO_META))
                .thenReturn(List.of(h1, h2, habito(102)));

        LogroHitoResponse r = servicio.lograrHito(100, COMPANERO);

        assertEquals(HOY, r.hito().fechaLogrado());
        assertEquals("Daniel Santos", r.hito().logradoPor());
        assertEquals(1, r.avance().logrados());
        assertEquals(2, r.avance().total(), "los hábitos no cuentan para el avance");
        assertEquals(50, r.avance().porcentaje());
        assertFalse(r.avance().completo());
        verify(subMetaRepository).save(h1);
    }

    @Test
    void lograrDeNuevoConservaLaFechaOriginal() throws Exception {
        SubMeta h = hito(100, "Terminar el libro 1");
        h.lograr(companero, HOY.minusDays(3));
        when(subMetaRepository.findById(100)).thenReturn(Optional.of(h));
        when(metaRepository.esParticipanteAceptado(CODIGO_META, CREADOR)).thenReturn(true);
        when(subMetaRepository.findByMetaCodigoMetaOrderByCodigoSubMetaAsc(CODIGO_META)).thenReturn(List.of(h));

        LogroHitoResponse r = servicio.lograrHito(100, CREADOR);

        assertEquals(HOY.minusDays(3), r.hito().fechaLogrado());
        assertTrue(r.avance().completo(), "con todos los hitos logrados la app propone cumplir la meta");
        verify(subMetaRepository, never()).save(any());
    }

    @Test
    void deshacerDevuelveElHitoAPendiente() throws Exception {
        SubMeta h = hito(100, "Terminar el libro 1");
        h.lograr(companero, HOY);
        when(subMetaRepository.findById(100)).thenReturn(Optional.of(h));
        when(metaRepository.esParticipanteAceptado(CODIGO_META, CREADOR)).thenReturn(true);
        when(subMetaRepository.findByMetaCodigoMetaOrderByCodigoSubMetaAsc(CODIGO_META)).thenReturn(List.of(h));

        LogroHitoResponse r = servicio.deshacerHito(100, CREADOR);

        assertNull(r.hito().fechaLogrado());
        assertNull(r.hito().logradoPor());
        assertEquals(0, r.avance().porcentaje());
    }

    @Test
    void quienNoParticipaNoMarcaHitos() throws Exception {
        when(subMetaRepository.findById(100)).thenReturn(Optional.of(hito(100, "Terminar el libro 1")));
        when(metaRepository.esParticipanteAceptado(CODIGO_META, AJENO)).thenReturn(false);

        assertThrows(PermisoDenegadoException.class, () -> servicio.lograrHito(100, AJENO));
        verify(subMetaRepository, never()).save(any());
    }

    @Test
    void unHabitoNoSeMarcaComoLogrado() throws Exception {
        when(subMetaRepository.findById(102)).thenReturn(Optional.of(habito(102)));
        when(metaRepository.esParticipanteAceptado(CODIGO_META, CREADOR)).thenReturn(true);

        ValidacionException e = assertThrows(ValidacionException.class, () -> servicio.lograrHito(102, CREADOR));
        assertEquals(List.of("Solo los hitos se marcan como logrados; los hábitos se registran por día"), e.getErrores());
    }

    @Test
    void enUnaMetaCumplidaLosHitosSonSoloLectura() throws Exception {
        SubMeta h = hito(100, "Terminar el libro 1");
        meta.marcarCumplida(CREADOR, HOY);
        when(subMetaRepository.findById(100)).thenReturn(Optional.of(h));
        when(metaRepository.esParticipanteAceptado(CODIGO_META, CREADOR)).thenReturn(true);

        ValidacionException e = assertThrows(ValidacionException.class, () -> servicio.lograrHito(100, CREADOR));
        assertEquals(List.of(SubMetaService.MENSAJE_SOLO_LECTURA), e.getErrores());
    }

    // ---------- HU29: avance ----------

    @Test
    void sinHitosNoHayPorcentajeDeAvance() throws Exception {
        when(subMetaRepository.findByMetaCodigoMetaOrderByCodigoSubMetaAsc(CODIGO_META)).thenReturn(List.of(habito(102)));

        assertNull(servicio.avance(CODIGO_META).porcentaje());
    }
}
