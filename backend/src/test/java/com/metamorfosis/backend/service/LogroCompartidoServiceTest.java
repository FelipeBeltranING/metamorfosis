package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.LogroCompartidoResponse;
import com.metamorfosis.backend.dto.LogroPublicoResponse;
import com.metamorfosis.backend.exception.LogroNoEncontradoException;
import com.metamorfosis.backend.exception.MetaNoEncontradaException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.LogroCompartido;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.LogroCompartidoRepository;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** HU21: caja blanca de LogroCompartidoService (compartir el logro y verlo desde el enlace público). */
@ExtendWith(MockitoExtension.class)
class LogroCompartidoServiceTest {

    private static final Integer CREADOR = 1;
    private static final Integer AJENO = 2;
    private static final Integer CODIGO_META = 10;
    private static final LocalDate FECHA_CUMPLIDA = LocalDate.of(2026, 11, 3);
    /** El alfabeto del servicio: sin 0/O ni 1/l/I. */
    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private LogroCompartidoRepository logroRepository;

    private LogroCompartidoService servicio;
    private Usuario creador;
    private Meta meta;

    @BeforeEach
    void setUp() throws Exception {
        // La URL viene con "/" final a propósito: el servicio debe quitarla.
        servicio = new LogroCompartidoService(metaRepository, usuarioRepository, logroRepository,
                "https://metamorfosis.test/");

        creador = new Usuario("Laura", "Gómez", "laura@test.com", "hash");
        creador.setCodigoUsuario(CREADOR);

        meta = new Meta("Ahorrar el fondo de emergencia", Plazo.LARGO, TipoMeta.FINANCIERA);
        meta.crear(creador);
        Field id = Meta.class.getDeclaredField("codigoMeta");
        id.setAccessible(true);
        id.set(meta, CODIGO_META);
    }

    private void metaCumplida() {
        meta.marcarCumplida(CREADOR, FECHA_CUMPLIDA);
    }

    @Test
    void metaInexistenteLanza404SinBuscarElLogro() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.empty());

        assertThrows(MetaNoEncontradaException.class, () -> servicio.compartirLogro(CODIGO_META, CREADOR));
        verifyNoInteractions(logroRepository);
    }

    @Test
    void quienNoParticipaNoComparteYNoSeGuardaNada() {
        metaCumplida();
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(metaRepository.esParticipanteAceptado(CODIGO_META, AJENO)).thenReturn(false);

        PermisoDenegadoException e = assertThrows(PermisoDenegadoException.class,
                () -> servicio.compartirLogro(CODIGO_META, AJENO));
        assertEquals("Solo un participante de la meta puede compartir el logro", e.getMessage());
        verifyNoInteractions(logroRepository);
    }

    @Test
    void elPermisoSeRevisaAntesQueElEstado() {
        // Meta activa y usuario ajeno: gana el 403, no el 400.
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(metaRepository.esParticipanteAceptado(CODIGO_META, AJENO)).thenReturn(false);

        assertThrows(PermisoDenegadoException.class, () -> servicio.compartirLogro(CODIGO_META, AJENO));
    }

    @Test
    void unaMetaActivaNoSeComparte() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(metaRepository.esParticipanteAceptado(CODIGO_META, CREADOR)).thenReturn(true);

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> servicio.compartirLogro(CODIGO_META, CREADOR));
        assertEquals("Solo se puede compartir una meta cumplida", e.getErrores().get(0));
        verifyNoInteractions(logroRepository);
    }

    @Test
    void compartirPorPrimeraVezCreaElEnlaceConUnTokenDeDiezCaracteresSinAmbiguos() {
        metaCumplida();
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(metaRepository.esParticipanteAceptado(CODIGO_META, CREADOR)).thenReturn(true);
        when(usuarioRepository.getReferenceById(CREADOR)).thenReturn(creador);
        when(logroRepository.findByMetaAndUsuario(meta, creador)).thenReturn(Optional.empty());
        when(logroRepository.findByToken(anyString())).thenReturn(Optional.empty());
        when(logroRepository.save(any(LogroCompartido.class))).thenAnswer(i -> i.getArgument(0));

        LogroCompartidoResponse r = servicio.compartirLogro(CODIGO_META, CREADOR);

        ArgumentCaptor<LogroCompartido> guardado = ArgumentCaptor.forClass(LogroCompartido.class);
        verify(logroRepository).save(guardado.capture());
        String token = guardado.getValue().getToken();
        assertEquals(LogroCompartidoService.LONGITUD_TOKEN, token.length());
        assertTrue(token.chars().allMatch(c -> ALFABETO.indexOf(c) >= 0), "token con caracteres ambiguos: " + token);
        assertEquals(token, r.token());
        assertEquals("https://metamorfosis.test/logros/" + token, r.url());
        assertEquals("Ahorrar el fondo de emergencia", r.nombreMeta());
        assertEquals(FECHA_CUMPLIDA, r.fechaCumplida());
    }

    @Test
    void compartirOtraVezReutilizaElEnlaceSinGuardarOtro() {
        metaCumplida();
        LogroCompartido existente = new LogroCompartido(meta, creador, "AbCdEfGh23");
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(metaRepository.esParticipanteAceptado(CODIGO_META, CREADOR)).thenReturn(true);
        when(usuarioRepository.getReferenceById(CREADOR)).thenReturn(creador);
        when(logroRepository.findByMetaAndUsuario(meta, creador)).thenReturn(Optional.of(existente));

        LogroCompartidoResponse r = servicio.compartirLogro(CODIGO_META, CREADOR);

        assertEquals("AbCdEfGh23", r.token());
        verify(logroRepository, never()).save(any());
    }

    @Test
    void siElTokenGeneradoYaExisteSeGeneraOtro() {
        metaCumplida();
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(metaRepository.esParticipanteAceptado(CODIGO_META, CREADOR)).thenReturn(true);
        when(usuarioRepository.getReferenceById(CREADOR)).thenReturn(creador);
        when(logroRepository.findByMetaAndUsuario(meta, creador)).thenReturn(Optional.empty());
        // El primer token generado ya existe; el segundo está libre.
        when(logroRepository.findByToken(anyString()))
                .thenReturn(Optional.of(new LogroCompartido(meta, creador, "ocupado")))
                .thenReturn(Optional.empty());
        when(logroRepository.save(any(LogroCompartido.class))).thenAnswer(i -> i.getArgument(0));

        servicio.compartirLogro(CODIGO_META, CREADOR);

        verify(logroRepository, times(2)).findByToken(anyString());
        verify(logroRepository).save(any(LogroCompartido.class));
    }

    @Test
    void elLogroPublicoMuestraLaMetaYElNombreSinElApellidoCompleto() {
        metaCumplida();
        when(logroRepository.findByToken("AbCdEfGh23"))
                .thenReturn(Optional.of(new LogroCompartido(meta, creador, "AbCdEfGh23")));

        LogroPublicoResponse r = servicio.obtenerLogroPublico("AbCdEfGh23");

        assertEquals("Ahorrar el fondo de emergencia", r.nombreMeta());
        assertEquals(TipoMeta.FINANCIERA, r.tipo());
        assertEquals(Plazo.LARGO, r.plazo());
        assertEquals(FECHA_CUMPLIDA, r.fechaCumplida());
        assertEquals("Laura G.", r.nombreVisible());
    }

    @Test
    void unTokenQueNoExisteLanza404() {
        when(logroRepository.findByToken("no-existe")).thenReturn(Optional.empty());

        LogroNoEncontradoException e = assertThrows(LogroNoEncontradoException.class,
                () -> servicio.obtenerLogroPublico("no-existe"));
        assertEquals("Este logro no existe o el enlace no es válido", e.getMessage());
    }

    @Test
    void nombreVisibleSinApellidoQuedaSoloElNombre() {
        Usuario sinApellido = new Usuario("  Mateo ", " ", "mateo@test.com", "hash");
        assertEquals("Mateo", LogroCompartidoService.nombreVisible(sinApellido));
        Usuario conApellido = new Usuario("Mateo", "ruiz", "mateo@test.com", "hash");
        assertEquals("Mateo r.", LogroCompartidoService.nombreVisible(conApellido));
    }
}
