package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.CuentaResponse;
import com.metamorfosis.backend.dto.MascotaRequest;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.Mascota;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HU20: caja blanca de CuentaService.cambiarMascota (elegir mascota gratis). */
@ExtendWith(MockitoExtension.class)
class CuentaServiceMascotaTest {

    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private CuentaService cuentaService;

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuario = new Usuario("Samuel", "Martínez", "samuel@correo.com", "hash");
        usuario.setCodigoUsuario(1);
    }

    @Test
    void todoUsuarioNuevoEmpiezaConLaMariposa() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        assertEquals(Mascota.MARIPOSA, cuentaService.verCuenta(1).mascota());
    }

    @Test
    void cambiaLaMascotaSinCostoYLaDevuelveEnLaCuenta() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        CuentaResponse cuenta = cuentaService.cambiarMascota(1, new MascotaRequest(" luciernaga "));

        assertEquals(Mascota.LUCIERNAGA, usuario.getMascota());
        assertEquals(Mascota.LUCIERNAGA, cuenta.mascota());
    }

    @Test
    void unaMascotaQueNoExisteNoSeGuarda() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> cuentaService.cambiarMascota(1, new MascotaRequest("DRAGON")));

        assertEquals(List.of("Elige una mascota válida"), e.getErrores());
        assertEquals(Mascota.MARIPOSA, usuario.getMascota());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void sinPedidoEsInvalido() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        assertThrows(ValidacionException.class, () -> cuentaService.cambiarMascota(1, null));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void usuarioInexistenteLanza404() {
        when(usuarioRepository.findById(9)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class,
                () -> cuentaService.cambiarMascota(9, new MascotaRequest("ABEJA")));
    }
}
