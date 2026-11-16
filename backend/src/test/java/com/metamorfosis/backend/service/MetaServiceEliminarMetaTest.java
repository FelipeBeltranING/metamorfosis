package com.metamorfosis.backend.service;

import com.metamorfosis.backend.config.CalendarioUsuario;
import com.metamorfosis.backend.exception.MetaNoEncontradaException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
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
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HU4: caja blanca de MetaService.eliminarMeta (plan de pruebas de la sección 16.4). */
@ExtendWith(MockitoExtension.class)
class MetaServiceEliminarMetaTest {

    private static final Integer CREADOR = 1;
    private static final Integer OTRO = 2;
    private static final Integer CODIGO_META = 10;

    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private InsigniaService insigniaService;
    @Mock private CalendarioUsuario calendario;

    @InjectMocks
    private MetaService metaService;

    private Meta meta;

    @BeforeEach
    void setUp() {
        Usuario creador = new Usuario("Daniel", "Santos", "daniel@test.com", "hash");
        creador.setCodigoUsuario(CREADOR);
        meta = new Meta("Correr 5 km", Plazo.MEDIANO, TipoMeta.SALUD_BIENESTAR);
        meta.crear(creador);
    }

    @Test
    void metaInexistenteLanza404SinBorrar() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.empty());

        assertThrows(MetaNoEncontradaException.class, () -> metaService.eliminarMeta(CODIGO_META, CREADOR));
        verify(metaRepository, never()).delete(any());
    }

    @Test
    void usuarioQueNoEsElCreadorLanza403SinBorrar() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        PermisoDenegadoException e = assertThrows(PermisoDenegadoException.class,
                () -> metaService.eliminarMeta(CODIGO_META, OTRO));
        assertEquals("Solo el creador puede eliminar la meta", e.getMessage());
        verify(metaRepository, never()).delete(any());
    }

    @Test
    void elCreadorLaEliminaUnaSolaVez() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        metaService.eliminarMeta(CODIGO_META, CREADOR);

        verify(metaRepository, times(1)).delete(meta);
        verify(metaRepository, never()).save(any());
    }

    @Test
    void unaMetaCumplidaTambienSePuedeEliminar() {
        meta.marcarCumplida(CREADOR, LocalDate.now());
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta));

        metaService.eliminarMeta(CODIGO_META, CREADOR);

        verify(metaRepository).delete(meta);
    }

    @Test
    void eliminarDosVecesLaMismaMetaResponde404LaSegunda() {
        when(metaRepository.findById(CODIGO_META)).thenReturn(Optional.of(meta), Optional.empty());

        metaService.eliminarMeta(CODIGO_META, CREADOR);
        assertThrows(MetaNoEncontradaException.class, () -> metaService.eliminarMeta(CODIGO_META, CREADOR));
        verify(metaRepository, times(1)).delete(meta);
    }
}
