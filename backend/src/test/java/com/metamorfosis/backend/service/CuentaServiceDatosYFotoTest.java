package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.CuentaResponse;
import com.metamorfosis.backend.dto.EditarCuentaRequest;
import com.metamorfosis.backend.dto.FotoPerfilRequest;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HU23: caja blanca de CuentaService.verCuenta, editarDatos, cambiarFoto y quitarFoto. */
@ExtendWith(MockitoExtension.class)
class CuentaServiceDatosYFotoTest {

    private static final String MENSAJE_FORMATO = "La foto debe ser una imagen JPG, PNG o WebP";
    private static final String MENSAJE_PESO = "La foto pesa demasiado; elige una más liviana";

    @Mock private UsuarioRepository usuarioRepository;
    @InjectMocks private CuentaService cuentaService;

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuario = new Usuario("Samuel", "Martínez", "samuel@correo.com", "hash");
        usuario.setCodigoUsuario(1);
    }

    /** Data URL con la firma real del formato al inicio y relleno hasta {@code largo} bytes. */
    private static String dataUrl(String tipo, int largo) {
        byte[] b = new byte[largo];
        byte[] firma = switch (tipo) {
            case "jpeg" -> new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
            case "png" -> new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0};
            default -> new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0};
        };
        System.arraycopy(firma, 0, b, 0, firma.length);
        return "data:image/" + tipo + ";base64," + Base64.getEncoder().encodeToString(b);
    }

    private void conUsuario() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(usuario));
    }

    private static void assertError(String esperado, Runnable accion) {
        ValidacionException e = assertThrows(ValidacionException.class, accion::run);
        assertEquals(List.of(esperado), e.getErrores());
    }

    // ---- Ver y editar los datos ----

    @Test
    void verCuentaDevuelveLosDatosDelUsuario() {
        conUsuario();

        CuentaResponse cuenta = cuentaService.verCuenta(1);

        assertEquals("Samuel", cuenta.nombre());
        assertEquals("Martínez", cuenta.apellido());
        assertEquals("samuel@correo.com", cuenta.email());
        assertNull(cuenta.foto());
    }

    @Test
    void usuarioInexistenteLanza404() {
        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(UsuarioNoEncontradoException.class, () -> cuentaService.verCuenta(99));
        assertThrows(UsuarioNoEncontradoException.class,
                () -> cuentaService.editarDatos(99, new EditarCuentaRequest("A", "B")));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void editarGuardaElNombreYElApellidoSinEspaciosDeMas() {
        conUsuario();
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        CuentaResponse cuenta = cuentaService.editarDatos(1, new EditarCuentaRequest("  Samuel   David ", " Martínez  Rivera "));

        assertEquals("Samuel David", cuenta.nombre());
        assertEquals("Martínez Rivera", cuenta.apellido());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void nombreYApellidoVaciosDanLosDosErroresYNoSeGuarda() {
        conUsuario();

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> cuentaService.editarDatos(1, new EditarCuentaRequest("   ", null)));

        assertEquals(List.of("Escribe tu nombre", "Escribe tu apellido"), e.getErrores());
        verify(usuarioRepository, never()).save(any());
        assertEquals("Samuel", usuario.getNombre());
    }

    @Test
    void sinPedidoFaltanLosDosCampos() {
        conUsuario();

        ValidacionException e = assertThrows(ValidacionException.class, () -> cuentaService.editarDatos(1, null));

        assertEquals(List.of("Escribe tu nombre", "Escribe tu apellido"), e.getErrores());
    }

    @Test
    void cincuentaCaracteresSePermitenYCincuentaYUnoNo() {
        conUsuario();
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
        String cincuenta = "a".repeat(CuentaService.TEXTO_MAXIMO);

        assertEquals(cincuenta, cuentaService.editarDatos(1, new EditarCuentaRequest(cincuenta, "B")).nombre());

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> cuentaService.editarDatos(1, new EditarCuentaRequest("A", cincuenta + "a")));
        assertEquals(List.of("El apellido puede tener máximo 50 caracteres"), e.getErrores());
    }

    // ---- Foto ----

    @Test
    void guardaUnaFotoDeCadaFormatoPermitido() {
        conUsuario();
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        for (String tipo : List.of("jpeg", "png", "webp")) {
            String foto = dataUrl(tipo, 64);
            assertEquals(foto, cuentaService.cambiarFoto(1, new FotoPerfilRequest(foto)).foto(), tipo);
        }
    }

    @Test
    void sinFotoPideElegirUna() {
        assertError("Elige una foto", () -> CuentaService.validarFoto(null));
        assertError("Elige una foto", () -> CuentaService.validarFoto("   "));
    }

    @Test
    void unaDataUrlQueNoEsDeImagenSeRechaza() {
        assertError(MENSAJE_FORMATO, () -> CuentaService.validarFoto("hola"));
        assertError(MENSAJE_FORMATO, () -> CuentaService.validarFoto("data:image/gif;base64,R0lGODlh"));
        assertError(MENSAJE_FORMATO, () -> CuentaService.validarFoto("data:text/plain;base64,aG9sYQ=="));
    }

    @Test
    void losBytesTienenQueCoincidirConElFormatoDeclarado() {
        // Dice PNG pero los bytes son de un JPG.
        String disfrazada = dataUrl("jpeg", 64).replace("image/jpeg", "image/png");
        assertError(MENSAJE_FORMATO, () -> CuentaService.validarFoto(disfrazada));
    }

    @Test
    void unaFotoEnElLimiteSePermiteYUnByteMasNo() {
        assertNotNull(CuentaService.validarFoto(dataUrl("png", CuentaService.FOTO_MAXIMA_BYTES)));
        assertError(MENSAJE_PESO, () -> CuentaService.validarFoto(dataUrl("png", CuentaService.FOTO_MAXIMA_BYTES + 1)));
    }

    @Test
    void unaFotoInvalidaNoReemplazaLaAnterior() {
        String anterior = dataUrl("jpeg", 64);
        usuario.setFotoPerfil(anterior);
        conUsuario();

        assertThrows(ValidacionException.class, () -> cuentaService.cambiarFoto(1, new FotoPerfilRequest("hola")));

        assertEquals(anterior, usuario.getFotoPerfil());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void quitarLaFotoLaDejaVaciaYGuarda() {
        usuario.setFotoPerfil(dataUrl("jpeg", 64));
        conUsuario();
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        assertNull(cuentaService.quitarFoto(1).foto());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void quitarSinFotoNoGuardaNada() {
        conUsuario();

        assertNull(cuentaService.quitarFoto(1).foto());
        verify(usuarioRepository, never()).save(any());
    }
}
