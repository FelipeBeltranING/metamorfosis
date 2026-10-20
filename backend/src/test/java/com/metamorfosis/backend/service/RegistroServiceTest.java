package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.RegistrarCumplimientoRequest;
import com.metamorfosis.backend.dto.RegistroResponse;
import com.metamorfosis.backend.exception.MetaNoEncontradaException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.Registro;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.RegistroRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HU5: registrar el cumplimiento de una meta en una fecha determinada. */
@ExtendWith(MockitoExtension.class)
class RegistroServiceTest {

    @Mock private RegistroRepository registroRepository;
    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private RegistroService registroService;

    private Meta meta;
    private Usuario usuario;

    @BeforeEach
    void setUp() throws Exception {
        usuario = new Usuario("Samuel", "Martinez", "samuel@test.com", "hash");
        setId(usuario, "codigoUsuario", 1);

        meta = new Meta("Leer 20 páginas", Plazo.CORTO, TipoMeta.ACADEMICA);
        setId(meta, "codigoMeta", 10);
    }

    private static void setId(Object objetivo, String campo, Integer valor) throws Exception {
        Field f = objetivo.getClass().getDeclaredField(campo);
        f.setAccessible(true);
        f.set(objetivo, valor);
    }

    @Test
    void registraElCumplimientoCuandoNoExistiaUnRegistroEseDia() {
        LocalDate fecha = LocalDate.now();
        var solicitud = new RegistrarCumplimientoRequest(fecha, true);

        when(metaRepository.findById(10)).thenReturn(Optional.of(meta));
        when(metaRepository.esParticipanteAceptado(10, 1)).thenReturn(true);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(registroRepository.findByMeta_CodigoMetaAndUsuario_CodigoUsuarioAndFechaRegistro(10, 1, fecha))
                .thenReturn(Optional.empty());
        when(registroRepository.save(any(Registro.class))).thenAnswer(inv -> {
            Registro r = inv.getArgument(0);
            setId(r, "codigoRegistro", 99);
            return r;
        });

        RegistroResponse respuesta = registroService.registrarCumplimiento(10, solicitud, 1);

        assertEquals(99, respuesta.codigoRegistro());
        assertEquals(10, respuesta.codigoMeta());
        assertEquals(1, respuesta.codigoUsuario());
        assertEquals(fecha, respuesta.fechaFinalizacion());
        assertTrue(respuesta.cumplimiento());
        verify(registroRepository).save(any(Registro.class));
    }

    @Test
    void actualizaElRegistroExistenteEnVezDeDuplicarlo() throws Exception {
        LocalDate fecha = LocalDate.now();
        var solicitud = new RegistrarCumplimientoRequest(fecha, true);

        Registro existente = new Registro(meta, usuario, fecha);
        setId(existente, "codigoRegistro", 55);

        when(metaRepository.findById(10)).thenReturn(Optional.of(meta));
        when(metaRepository.esParticipanteAceptado(10, 1)).thenReturn(true);
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(registroRepository.findByMeta_CodigoMetaAndUsuario_CodigoUsuarioAndFechaRegistro(10, 1, fecha))
                .thenReturn(Optional.of(existente));
        when(registroRepository.save(any(Registro.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistroResponse respuesta = registroService.registrarCumplimiento(10, solicitud, 1);

        assertEquals(55, respuesta.codigoRegistro());
        assertTrue(respuesta.cumplimiento());
        verify(registroRepository, never()).findById(any());
    }

    @Test
    void rechazaFechaFutura() {
        var solicitud = new RegistrarCumplimientoRequest(LocalDate.now().plusDays(1), true);

        assertThrows(ValidacionException.class,
                () -> registroService.registrarCumplimiento(10, solicitud, 1));
        verifyNoInteractions(registroRepository);
    }

    @Test
    void rechazaFechaNula() {
        var solicitud = new RegistrarCumplimientoRequest(null, true);

        assertThrows(ValidacionException.class,
                () -> registroService.registrarCumplimiento(10, solicitud, 1));
    }

    @Test
    void rechazaCumplimientoNulo() {
        var solicitud = new RegistrarCumplimientoRequest(LocalDate.now(), null);

        assertThrows(ValidacionException.class,
                () -> registroService.registrarCumplimiento(10, solicitud, 1));
    }

    @Test
    void lanzaExcepcionSiLaMetaNoExiste() {
        var solicitud = new RegistrarCumplimientoRequest(LocalDate.now(), true);
        when(metaRepository.findById(404)).thenReturn(Optional.empty());

        assertThrows(MetaNoEncontradaException.class,
                () -> registroService.registrarCumplimiento(404, solicitud, 1));
    }

    @Test
    void rechazaMetaCumplida() {
        // Fuerza estado CUMPLIDA vía reflexión: no existe setter público (regla de negocio deliberada).
        setEstadoCumplida(meta);
        var solicitud = new RegistrarCumplimientoRequest(LocalDate.now(), true);

        when(metaRepository.findById(10)).thenReturn(Optional.of(meta));

        assertThrows(ValidacionException.class,
                () -> registroService.registrarCumplimiento(10, solicitud, 1));
        verify(metaRepository, never()).esParticipanteAceptado(any(), any());
    }

    @Test
    void rechazaUsuarioQueNoEsParticipanteAceptado() {
        var solicitud = new RegistrarCumplimientoRequest(LocalDate.now(), true);

        when(metaRepository.findById(10)).thenReturn(Optional.of(meta));
        when(metaRepository.esParticipanteAceptado(10, 2)).thenReturn(false);

        assertThrows(PermisoDenegadoException.class,
                () -> registroService.registrarCumplimiento(10, solicitud, 2));
        verifyNoInteractions(registroRepository);
    }

    private static void setEstadoCumplida(Meta meta) {
        try {
            Field f = Meta.class.getDeclaredField("estado");
            f.setAccessible(true);
            f.set(meta, EstadoMeta.CUMPLIDA);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
