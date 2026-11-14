package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.AvatarPerfil;
import com.metamorfosis.backend.dto.CuentaResponse;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.CatalogoAvatar;
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

/** HU20: caja blanca de CuentaService.cambiarAvatar (armar el avatar con piezas del catálogo). */
@ExtendWith(MockitoExtension.class)
class CuentaServiceAvatarTest {

    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private CuentaService cuentaService;

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuario = new Usuario("Samuel", "Martínez", "samuel@correo.com", "hash");
        usuario.setCodigoUsuario(1);
    }

    @Test
    void todoUsuarioNuevoEmpiezaConElAvatarPorDefecto() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        AvatarPerfil avatar = cuentaService.verCuenta(1).avatar();

        assertEquals(new AvatarPerfil(CatalogoAvatar.PIEL_INICIAL, CatalogoAvatar.PEINADO_INICIAL,
                CatalogoAvatar.COLOR_PELO_INICIAL, CatalogoAvatar.OJOS_INICIALES, CatalogoAvatar.BOCA_INICIAL, null), avatar);
    }

    @Test
    void guardaTodasLasPiezasYLasDevuelveEnLaCuenta() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        AvatarPerfil pedido = new AvatarPerfil(" 8c5a2b ", "froBun", "220f00", "starstruck", "gapSmile", "glasses");
        CuentaResponse cuenta = cuentaService.cambiarAvatar(1, pedido);

        AvatarPerfil esperado = new AvatarPerfil("8c5a2b", "froBun", "220f00", "starstruck", "gapSmile", "glasses");
        assertEquals(esperado, cuenta.avatar());
        assertEquals("froBun", usuario.getAvatar().getPeinado());
    }

    @Test
    void elAccesorioEsOpcional() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        CuentaResponse cuenta = cuentaService.cambiarAvatar(1,
                new AvatarPerfil("ffe4c0", "braids", "e9b729", "normal", "braces", "  "));

        assertNull(cuenta.avatar().accesorio());
    }

    @Test
    void piezasQueNoEstanEnElCatalogoNoSeGuardanYSeExplicaCadaUna() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        ValidacionException e = assertThrows(ValidacionException.class, () -> cuentaService.cambiarAvatar(1,
                new AvatarPerfil("000000", "shortHair", "71472d", "laser", "openedSmile", "corona")));

        assertEquals(List.of("Elige un tono de piel válido", "Elige unos ojos válidos", "Elige un accesorio válido"),
                e.getErrores());
        assertEquals(CatalogoAvatar.PIEL_INICIAL, usuario.getAvatar().getPiel());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void sinPedidoFaltanTodasLasPiezasObligatorias() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));

        ValidacionException e = assertThrows(ValidacionException.class, () -> cuentaService.cambiarAvatar(1, null));

        assertEquals(5, e.getErrores().size());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void usuarioInexistenteLanza404() {
        when(usuarioRepository.findById(9)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> cuentaService.cambiarAvatar(9,
                new AvatarPerfil("ffe4c0", "braids", "e9b729", "normal", "braces", null)));
    }
}
