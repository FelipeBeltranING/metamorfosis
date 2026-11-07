package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.AparienciaRequest;
import com.metamorfosis.backend.dto.CuentaResponse;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.ColorAcento;
import com.metamorfosis.backend.model.ModoTema;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HU19: caja blanca de CuentaService.cambiarApariencia. */
@ExtendWith(MockitoExtension.class)
class CuentaServiceAparienciaTest {

    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private CuentaService cuentaService;

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuario = new Usuario("Daniel", "Santos", "daniel@correo.com", "hash");
        usuario.setCodigoUsuario(1);
    }

    @Test
    void usuarioNuevoTieneLaAparienciaDeSiempre() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        CuentaResponse cuenta = cuentaService.verCuenta(1);

        assertEquals(ModoTema.CLARO, cuenta.apariencia().modo());
        assertEquals(ColorAcento.VIOLETA, cuenta.apariencia().acento());
    }

    @Test
    void guardaModoYAcento() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        CuentaResponse cuenta = cuentaService.cambiarApariencia(1, new AparienciaRequest("OSCURO", "AZUL"));

        assertEquals(ModoTema.OSCURO, usuario.getModoTema());
        assertEquals(ColorAcento.AZUL, usuario.getColorAcento());
        assertEquals(new CuentaResponse.Apariencia(ModoTema.OSCURO, ColorAcento.AZUL), cuenta.apariencia());
    }

    @Test
    void aceptaMinusculasYEspacios() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        cuentaService.cambiarApariencia(1, new AparienciaRequest(" automatico ", "esmeralda"));

        assertEquals(ModoTema.AUTOMATICO, usuario.getModoTema());
        assertEquals(ColorAcento.ESMERALDA, usuario.getColorAcento());
    }

    @Test
    void valoresInvalidosNoGuardanYDicenQueFalla() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> cuentaService.cambiarApariencia(1, new AparienciaRequest("NEON", null)));

        assertEquals(2, e.getErrores().size());
        assertEquals(ModoTema.CLARO, usuario.getModoTema());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void sinPedidoEsInvalido() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        assertThrows(ValidacionException.class, () -> cuentaService.cambiarApariencia(1, null));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void usuarioInexistente() {
        when(usuarioRepository.findById(9)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> cuentaService.cambiarApariencia(9, new AparienciaRequest("OSCURO", "AZUL")));
    }
}
