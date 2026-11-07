package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.dto.VisibilidadRequest;
import com.metamorfosis.backend.exception.MetaNoEncontradaException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.EstadoInvitacion;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Participacion;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/** HU25: caja blanca de MetaService.cambiarVisibilidad (ocultar o mostrar una meta en el perfil). */
@ExtendWith(MockitoExtension.class)
class MetaServiceVisibilidadTest {

    private static final Integer CREADOR = 1;
    private static final Integer COMPANERO = 2;
    private static final Integer INVITADO = 3;
    private static final Integer AJENO = 4;
    private static final Integer CODIGO_META = 10;

    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private InsigniaService insigniaService;

    @InjectMocks
    private MetaService metaService;

    private Meta meta;

    @BeforeEach
    void setUp() throws Exception {
        meta = new Meta("Ahorrar el fondo de emergencia", Plazo.LARGO, TipoMeta.FINANCIERA);
        meta.crear(usuario(CREADOR));
        Field id = Meta.class.getDeclaredField("codigoMeta");
        id.setAccessible(true);
        id.set(meta, CODIGO_META);
    }

    private static Usuario usuario(Integer codigo) {
        Usuario u = new Usuario("Usuario" + codigo, "Prueba", "u" + codigo + "@test.com", "hash");
        u.setCodigoUsuario(codigo);
        return u;
    }

    /** Agrega un participante como lo harán las HU13 y HU14 (invitación y respuesta). */
    @SuppressWarnings("unchecked")
    private void agregarParticipante(Integer codigo, EstadoInvitacion estado) throws Exception {
        Field campo = Meta.class.getDeclaredField("participaciones");
        campo.setAccessible(true);
        ((List<Participacion>) campo.get(meta)).add(new Participacion(usuario(codigo), meta, estado));
    }

    private void guardarDevuelveLaMeta() {
        when(metaRepository.save(meta)).thenReturn(meta);
    }

    @Test
    void solicitudSinValorLanza400SinConsultarLaMeta() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> metaService.cambiarVisibilidad(CODIGO_META, new VisibilidadRequest(null), CREADOR));
        assertEquals(List.of("Indica si la meta queda oculta"), e.getErrores());
        verify(metaRepository, never()).findById(anyInt());
    }

    @Test
    void solicitudNulaLanza400() {
        assertThrows(ValidacionException.class,
                () -> metaService.cambiarVisibilidad(CODIGO_META, null, CREADOR));
    }

    @Test
    void metaInexistenteLanza404() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.empty());

        assertThrows(MetaNoEncontradaException.class,
                () -> metaService.cambiarVisibilidad(CODIGO_META, new VisibilidadRequest(true), CREADOR));
        verify(metaRepository, never()).save(any());
    }

    @Test
    void usuarioQueNoParticipaLanza403() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        PermisoDenegadoException e = assertThrows(PermisoDenegadoException.class,
                () -> metaService.cambiarVisibilidad(CODIGO_META, new VisibilidadRequest(true), AJENO));
        assertEquals("Solo los participantes de la meta pueden ocultarla", e.getMessage());
        assertFalse(meta.isOcultaPara(CREADOR));
        verify(metaRepository, never()).save(any());
    }

    @Test
    void invitacionPendienteLanza403() throws Exception {
        agregarParticipante(INVITADO, EstadoInvitacion.PENDIENTE);
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        assertThrows(PermisoDenegadoException.class,
                () -> metaService.cambiarVisibilidad(CODIGO_META, new VisibilidadRequest(true), INVITADO));
        assertFalse(meta.isOcultaPara(INVITADO));
        verify(metaRepository, never()).save(any());
    }

    @Test
    void elCreadorOcultaLaMeta() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        guardarDevuelveLaMeta();

        MetaResponse respuesta = metaService.cambiarVisibilidad(
                CODIGO_META, new VisibilidadRequest(true), CREADOR);

        assertTrue(respuesta.oculta());
        assertTrue(meta.isOcultaPara(CREADOR));
        assertEquals(EstadoMeta.ACTIVA, respuesta.estado());
        verify(metaRepository, times(1)).save(meta);
    }

    @Test
    void ocultarNoCambiaElEstadoNiLaFechaDeUnaMetaCumplida() {
        LocalDate fecha = LocalDate.now().minusDays(3);
        meta.marcarCumplida(CREADOR, fecha);
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        guardarDevuelveLaMeta();

        MetaResponse respuesta = metaService.cambiarVisibilidad(
                CODIGO_META, new VisibilidadRequest(true), CREADOR);

        assertTrue(respuesta.oculta());
        assertEquals(EstadoMeta.CUMPLIDA, respuesta.estado());
        assertEquals(fecha, respuesta.fechaCumplida());
    }

    @Test
    void mostrarDeNuevoQuitaLaMarca() {
        meta.cambiarVisibilidad(CREADOR, true);
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        guardarDevuelveLaMeta();

        MetaResponse respuesta = metaService.cambiarVisibilidad(
                CODIGO_META, new VisibilidadRequest(false), CREADOR);

        assertFalse(respuesta.oculta());
        assertFalse(meta.isOcultaPara(CREADOR));
    }

    @Test
    void ocultarDosVecesLaDejaOculta() {
        meta.cambiarVisibilidad(CREADOR, true);
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        guardarDevuelveLaMeta();

        MetaResponse respuesta = metaService.cambiarVisibilidad(
                CODIGO_META, new VisibilidadRequest(true), CREADOR);

        assertTrue(respuesta.oculta());
    }

    @Test
    void enUnaMetaCompartidaSoloCambiaLaParticipacionDeQuienLaOculta() throws Exception {
        agregarParticipante(COMPANERO, EstadoInvitacion.ACEPTADA);
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        guardarDevuelveLaMeta();

        MetaResponse respuesta = metaService.cambiarVisibilidad(
                CODIGO_META, new VisibilidadRequest(true), COMPANERO);

        assertTrue(respuesta.oculta());
        assertTrue(meta.isOcultaPara(COMPANERO));
        assertFalse(meta.isOcultaPara(CREADOR));
        assertFalse(MetaResponse.desde(meta, CREADOR).oculta());
        assertTrue(MetaResponse.desde(meta, COMPANERO).oculta());
        assertFalse(MetaResponse.desde(meta).oculta());
    }

    @Test
    void elLogRecibeElValorDeOculta() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        guardarDevuelveLaMeta();

        try (MockedStatic<ContextoAuditoria> contexto = mockStatic(ContextoAuditoria.class)) {
            metaService.cambiarVisibilidad(CODIGO_META, new VisibilidadRequest(true), CREADOR);
            contexto.verify(() -> ContextoAuditoria.agregar("oculta", true));
        }
    }
}
