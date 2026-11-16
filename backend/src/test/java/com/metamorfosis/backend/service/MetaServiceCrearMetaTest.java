package com.metamorfosis.backend.service;

import com.metamorfosis.backend.config.CalendarioUsuario;
import com.metamorfosis.backend.dto.CrearMetaRequest;
import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HU1: caja blanca de MetaService.crearMeta y de su validación (plan de pruebas de la sección 16.1). */
@ExtendWith(MockitoExtension.class)
class MetaServiceCrearMetaTest {

    private static final Integer USUARIO = 1;

    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private InsigniaService insigniaService;
    @Mock private CalendarioUsuario calendario;

    @InjectMocks
    private MetaService metaService;

    private List<String> errores(CrearMetaRequest solicitud) {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> metaService.crearMeta(solicitud, USUARIO));
        verifyNoInteractions(usuarioRepository, metaRepository);
        return e.getErrores();
    }

    private Usuario usuarioExistente() {
        Usuario usuario = new Usuario("Samuel", "Martinez", "samuel@test.com", "hash");
        usuario.setCodigoUsuario(USUARIO);
        when(usuarioRepository.findById(USUARIO)).thenReturn(Optional.of(usuario));
        when(metaRepository.save(any(Meta.class))).thenAnswer(i -> i.getArgument(0));
        return usuario;
    }

    @Test
    void solicitudNulaDevuelveLosTresErrores() {
        assertEquals(List.of("El nombre es obligatorio", "El plazo es obligatorio", "El tipo es obligatorio"),
                errores(null));
    }

    @Test
    void nombreNuloOEnBlancoEsObligatorio() {
        assertEquals(List.of("El nombre es obligatorio"),
                errores(new CrearMetaRequest(null, Plazo.CORTO, TipoMeta.PERSONAL)));
        assertEquals(List.of("El nombre es obligatorio"),
                errores(new CrearMetaRequest("   ", Plazo.CORTO, TipoMeta.PERSONAL)));
    }

    @Test
    void nombreDe201CaracteresSuperaElLimite() {
        assertEquals(List.of("El nombre no puede superar 200 caracteres"),
                errores(new CrearMetaRequest("a".repeat(201), Plazo.CORTO, TipoMeta.PERSONAL)));
    }

    @Test
    void elLimiteSeMideSinLosEspaciosDeLosExtremos() {
        usuarioExistente();
        MetaResponse creada = metaService.crearMeta(
                new CrearMetaRequest("a".repeat(200) + "     ", Plazo.CORTO, TipoMeta.PERSONAL), USUARIO);
        assertEquals(200, creada.nombre().length());
    }

    @Test
    void plazoYTipoSeValidanPorSeparadoYLosErroresSeAcumulan() {
        assertEquals(List.of("El plazo es obligatorio"),
                errores(new CrearMetaRequest("Leer", null, TipoMeta.PERSONAL)));
        assertEquals(List.of("El tipo es obligatorio"),
                errores(new CrearMetaRequest("Leer", Plazo.CORTO, null)));
        assertEquals(List.of("El nombre es obligatorio", "El plazo es obligatorio", "El tipo es obligatorio"),
                errores(new CrearMetaRequest("", null, null)));
    }

    @Test
    void usuarioInexistenteLanza404SinGuardar() {
        when(usuarioRepository.findById(USUARIO)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> metaService.crearMeta(
                new CrearMetaRequest("Leer 20 libros", Plazo.CORTO, TipoMeta.PERSONAL), USUARIO));
        verify(metaRepository, never()).save(any());
    }

    @Test
    void rutaFelizGuardaLaMetaActivaConLaParticipacionAceptadaDelCreador() {
        Usuario creador = usuarioExistente();

        MetaResponse creada = metaService.crearMeta(
                new CrearMetaRequest("  Leer 20 libros  ", Plazo.CORTO, TipoMeta.PERSONAL), USUARIO);

        ArgumentCaptor<Meta> guardada = ArgumentCaptor.forClass(Meta.class);
        verify(metaRepository, times(1)).save(guardada.capture());
        Meta meta = guardada.getValue();
        assertEquals("Leer 20 libros", meta.getNombre());
        assertEquals(EstadoMeta.ACTIVA, meta.getEstado());
        assertSame(creador, meta.getCreador());
        assertTrue(meta.participaAceptado(USUARIO), "el creador queda con su participación ACEPTADA");
        assertEquals(1, creada.participantes().size());
        assertEquals("Leer 20 libros", creada.nombre());
        assertEquals(EstadoMeta.ACTIVA, creada.estado());
    }

    @Test
    void aceptaTodosLosPlazosYTipos() {
        usuarioExistente();
        for (Plazo plazo : Plazo.values()) {
            for (TipoMeta tipo : TipoMeta.values()) {
                MetaResponse creada = metaService.crearMeta(new CrearMetaRequest("Meta", plazo, tipo), USUARIO);
                assertEquals(plazo, creada.plazo());
                assertEquals(tipo, creada.tipo());
            }
        }
        verify(metaRepository, times(Plazo.values().length * TipoMeta.values().length)).save(any());
    }
}
