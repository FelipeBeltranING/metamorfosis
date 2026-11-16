package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.config.CalendarioUsuario;
import com.metamorfosis.backend.dto.CrearMetaRequest;
import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.exception.MetaNoEncontradaException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Meta;
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
import static org.mockito.Mockito.*;

/**
 * HU2: caja blanca de MetaService.editarMeta. El orden de las decisiones es validar, buscar la meta,
 * comprobar el creador y comprobar el estado (plan de pruebas de la sección 16.2).
 */
@ExtendWith(MockitoExtension.class)
class MetaServiceEditarMetaTest {

    private static final Integer CREADOR = 1;
    private static final Integer OTRO = 2;
    private static final Integer CODIGO_META = 10;
    private static final CrearMetaRequest VALIDA = new CrearMetaRequest("  Leer 30 libros ", Plazo.MEDIANO, TipoMeta.ACADEMICA);

    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private InsigniaService insigniaService;
    @Mock private CalendarioUsuario calendario;

    @InjectMocks
    private MetaService metaService;

    private Meta meta;

    @BeforeEach
    void setUp() throws Exception {
        Usuario creador = new Usuario("Felipe", "Beltran", "felipe@test.com", "hash");
        creador.setCodigoUsuario(CREADOR);
        meta = new Meta("Leer 20 libros", Plazo.CORTO, TipoMeta.PERSONAL);
        meta.crear(creador);
        Field id = Meta.class.getDeclaredField("codigoMeta");
        id.setAccessible(true);
        id.set(meta, CODIGO_META);
    }

    @Test
    void validarVaAntesDeBuscarLaMeta() {
        ValidacionException e = assertThrows(ValidacionException.class, () ->
                metaService.editarMeta(CODIGO_META, new CrearMetaRequest("", Plazo.CORTO, TipoMeta.PERSONAL), CREADOR));
        assertEquals(List.of("El nombre es obligatorio"), e.getErrores());
        verifyNoInteractions(metaRepository);
    }

    @Test
    void nombreDe201CaracteresSeRechaza() {
        ValidacionException e = assertThrows(ValidacionException.class, () ->
                metaService.editarMeta(CODIGO_META, new CrearMetaRequest("a".repeat(201), Plazo.CORTO, TipoMeta.PERSONAL), CREADOR));
        assertEquals(List.of("El nombre no puede superar 200 caracteres"), e.getErrores());
    }

    @Test
    void metaInexistenteLanza404SinGuardar() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.empty());

        assertThrows(MetaNoEncontradaException.class, () -> metaService.editarMeta(CODIGO_META, VALIDA, CREADOR));
        verify(metaRepository, never()).save(any());
    }

    @Test
    void usuarioQueNoEsElCreadorLanza403() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        PermisoDenegadoException e = assertThrows(PermisoDenegadoException.class,
                () -> metaService.editarMeta(CODIGO_META, VALIDA, OTRO));
        assertEquals("Solo el creador puede editar la meta", e.getMessage());
        assertEquals("Leer 20 libros", meta.getNombre());
        verify(metaRepository, never()).save(any());
    }

    @Test
    void metaCumplidaNoSeEdita() {
        meta.marcarCumplida(CREADOR, LocalDate.now());
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> metaService.editarMeta(CODIGO_META, VALIDA, CREADOR));
        assertEquals(List.of("Una meta cumplida no se puede editar"), e.getErrores());
        verify(metaRepository, never()).save(any());
    }

    @Test
    void laDecisionDeCreadorVaAntesQueLaDeEstado() {
        meta.marcarCumplida(CREADOR, LocalDate.now());
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        assertThrows(PermisoDenegadoException.class, () -> metaService.editarMeta(CODIGO_META, VALIDA, OTRO));
    }

    @Test
    void elLogGuardaLosValoresAnteriores() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(metaRepository.save(any(Meta.class))).thenAnswer(i -> i.getArgument(0));

        try (MockedStatic<ContextoAuditoria> contexto = mockStatic(ContextoAuditoria.class)) {
            metaService.editarMeta(CODIGO_META, VALIDA, CREADOR);

            contexto.verify(() -> ContextoAuditoria.agregar("nombreAnterior", "Leer 20 libros"));
            contexto.verify(() -> ContextoAuditoria.agregar("plazoAnterior", Plazo.CORTO));
            contexto.verify(() -> ContextoAuditoria.agregar("tipoAnterior", TipoMeta.PERSONAL));
        }
    }

    @Test
    void rutaFelizCambiaNombrePlazoYTipoYNadaMas() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));
        when(metaRepository.save(any(Meta.class))).thenAnswer(i -> i.getArgument(0));

        MetaResponse editada = metaService.editarMeta(CODIGO_META, VALIDA, CREADOR);

        verify(metaRepository, times(1)).save(meta);
        assertEquals("Leer 30 libros", editada.nombre());
        assertEquals(Plazo.MEDIANO, editada.plazo());
        assertEquals(TipoMeta.ACADEMICA, editada.tipo());
        assertEquals(EstadoMeta.ACTIVA, editada.estado());
        assertFalse(editada.oculta());
        assertEquals(CREADOR, meta.getCreador().getCodigoUsuario());
    }
}
