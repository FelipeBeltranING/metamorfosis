package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.RestablecerContrasenaRequest;
import com.metamorfosis.backend.dto.SolicitarCodigoRequest;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.CodigoRecuperacion;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.ResultadoLog;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.CodigoRecuperacionRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import com.metamorfosis.backend.service.correo.CorreoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/** HU33: caja blanca de RecuperacionContrasenaService (pedir el código y cambiar la contraseña con él). */
@ExtendWith(MockitoExtension.class)
class RecuperacionContrasenaServiceTest {

    private static final String NUEVA = "Nueva2026x";
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 14, 21, 0);

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CodigoRecuperacionRepository codigoRepository;
    @Mock private ContrasenaService contrasenaService;
    @Mock private CorreoService correoService;
    @Mock private LogService logService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private RecuperacionContrasenaService servicio;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        servicio = new RecuperacionContrasenaService(usuarioRepository, codigoRepository, passwordEncoder,
                contrasenaService, correoService, logService, reloj);
        usuario = new Usuario("Daniel", "Santos", "daniel@correo.com", "hash");
        usuario.setCodigoUsuario(1);
        LocaleContextHolder.setLocale(Locale.forLanguageTag("es"));
    }

    @AfterEach
    void limpiarIdioma() {
        LocaleContextHolder.resetLocaleContext();
    }

    private CodigoRecuperacion codigoVigente(String codigo) {
        CodigoRecuperacion registro = new CodigoRecuperacion(1);
        registro.renovar(passwordEncoder.encode(codigo), AHORA.minusMinutes(2), AHORA.plusMinutes(13));
        return registro;
    }

    private RestablecerContrasenaRequest conCodigo(String codigo) {
        return new RestablecerContrasenaRequest("Daniel@Correo.com ", codigo, NUEVA, NUEVA);
    }

    // ---------- Paso 1: pedir el código ----------

    @Test
    void mandaUnCodigoDeSeisDigitosQueVenceEn15Minutos() {
        when(usuarioRepository.findByEmailIgnoreCase("daniel@correo.com")).thenReturn(Optional.of(usuario));
        when(codigoRepository.findById(1)).thenReturn(Optional.empty());

        servicio.solicitarCodigo(new SolicitarCodigoRequest(" Daniel@Correo.com"));

        ArgumentCaptor<CodigoRecuperacion> guardado = ArgumentCaptor.forClass(CodigoRecuperacion.class);
        verify(codigoRepository).save(guardado.capture());
        assertEquals(AHORA.plusMinutes(15), guardado.getValue().getExpiraEn());
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviar(eq("daniel@correo.com"),
                eq("Tu código para cambiar la contraseña de Metamorfosis"), anyString(), texto.capture());
        Matcher m = Pattern.compile("(\\d{3}) (\\d{3})").matcher(texto.getValue());
        assertTrue(m.find(), "el correo trae el código");
        // Se guarda cifrado: el código del correo coincide con el hash, pero no está en claro.
        String codigo = m.group(1) + m.group(2);
        assertTrue(passwordEncoder.matches(codigo, guardado.getValue().getCodigoHash()));
        assertNotEquals(codigo, guardado.getValue().getCodigoHash());
    }

    @Test
    void sinCuentaNoMandaNadaNiLoDice() {
        when(usuarioRepository.findByEmailIgnoreCase("nadie@correo.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> servicio.solicitarCodigo(new SolicitarCodigoRequest("nadie@correo.com")));

        verifyNoInteractions(correoService, codigoRepository);
        verify(logService).registrar(isNull(), eq(AccionLog.SOLICITAR_CODIGO_CONTRASENA), eq(EntidadLog.USUARIO),
                isNull(), eq(ResultadoLog.ERROR), contains("USUARIO_INEXISTENTE"));
    }

    @Test
    void noMandaOtroCodigoAntesDeUnMinuto() {
        when(usuarioRepository.findByEmailIgnoreCase("daniel@correo.com")).thenReturn(Optional.of(usuario));
        CodigoRecuperacion reciente = new CodigoRecuperacion(1);
        reciente.renovar("hash", AHORA.minusSeconds(30), AHORA.plusMinutes(14));
        when(codigoRepository.findById(1)).thenReturn(Optional.of(reciente));

        servicio.solicitarCodigo(new SolicitarCodigoRequest("daniel@correo.com"));

        verify(codigoRepository, never()).save(any());
        verifyNoInteractions(correoService);
    }

    @Test
    void pasadoElMinutoElCodigoNuevoReemplazaAlAnterior() {
        when(usuarioRepository.findByEmailIgnoreCase("daniel@correo.com")).thenReturn(Optional.of(usuario));
        CodigoRecuperacion anterior = new CodigoRecuperacion(1);
        anterior.renovar("hash-viejo", AHORA.minusMinutes(5), AHORA.plusMinutes(10));
        anterior.sumarIntento();
        when(codigoRepository.findById(1)).thenReturn(Optional.of(anterior));

        servicio.solicitarCodigo(new SolicitarCodigoRequest("daniel@correo.com"));

        assertNotEquals("hash-viejo", anterior.getCodigoHash());
        assertEquals(0, anterior.getIntentos());
        verify(codigoRepository).save(anterior);
        verify(correoService).enviar(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void elCorreoDebeTenerFormatoValido() {
        ValidacionException e = assertThrows(ValidacionException.class,
                () -> servicio.solicitarCodigo(new SolicitarCodigoRequest("no-es-correo")));

        assertEquals(List.of(RecuperacionContrasenaService.MENSAJE_EMAIL_INVALIDO), e.getErrores());
        verifyNoInteractions(usuarioRepository);
    }

    // ---------- Paso 2: cambiar la contraseña con el código ----------

    @Test
    void conElCodigoCorrectoCambiaLaContrasenaYGastaElCodigo() {
        when(usuarioRepository.findByEmailIgnoreCase("daniel@correo.com")).thenReturn(Optional.of(usuario));
        CodigoRecuperacion registro = codigoVigente("482913");
        when(codigoRepository.findById(1)).thenReturn(Optional.of(registro));

        servicio.restablecer(conCodigo("482 913"));

        verify(contrasenaService).ponerNueva(usuario, NUEVA);
        verify(codigoRepository).delete(registro);
        verify(logService).registrar(1, AccionLog.RESTABLECER_CONTRASENA, EntidadLog.USUARIO, 1, ResultadoLog.EXITO, null);
    }

    @Test
    void conElCodigoIncorrectoCuentaElIntentoYDiceCuantosQuedan() {
        when(usuarioRepository.findByEmailIgnoreCase("daniel@correo.com")).thenReturn(Optional.of(usuario));
        CodigoRecuperacion registro = codigoVigente("482913");
        when(codigoRepository.findById(1)).thenReturn(Optional.of(registro));

        ValidacionException e = assertThrows(ValidacionException.class, () -> servicio.restablecer(conCodigo("111111")));

        assertEquals(List.of("El código no es correcto. Te quedan 4 intento(s)"), e.getErrores());
        assertEquals(1, registro.getIntentos());
        verify(codigoRepository).save(registro);
        verifyNoInteractions(contrasenaService);
    }

    @Test
    void alQuintoIntentoFallidoElCodigoSeBorra() {
        when(usuarioRepository.findByEmailIgnoreCase("daniel@correo.com")).thenReturn(Optional.of(usuario));
        CodigoRecuperacion registro = codigoVigente("482913");
        for (int i = 0; i < 4; i++) registro.sumarIntento();
        when(codigoRepository.findById(1)).thenReturn(Optional.of(registro));

        ValidacionException e = assertThrows(ValidacionException.class, () -> servicio.restablecer(conCodigo("111111")));

        assertEquals(List.of(RecuperacionContrasenaService.MENSAJE_DEMASIADOS_INTENTOS), e.getErrores());
        verify(codigoRepository).delete(registro);
        verifyNoInteractions(contrasenaService);
    }

    @Test
    void unCodigoVencidoNoSirve() {
        when(usuarioRepository.findByEmailIgnoreCase("daniel@correo.com")).thenReturn(Optional.of(usuario));
        CodigoRecuperacion vencido = new CodigoRecuperacion(1);
        vencido.renovar(passwordEncoder.encode("482913"), AHORA.minusMinutes(20), AHORA.minusMinutes(5));
        when(codigoRepository.findById(1)).thenReturn(Optional.of(vencido));

        ValidacionException e = assertThrows(ValidacionException.class, () -> servicio.restablecer(conCodigo("482913")));

        assertEquals(List.of(RecuperacionContrasenaService.MENSAJE_CODIGO_NO_VALIDO), e.getErrores());
        verifyNoInteractions(contrasenaService);
    }

    @Test
    void unCorreoSinCuentaDaElMismoErrorQueUnCodigoVencido() {
        when(usuarioRepository.findByEmailIgnoreCase("nadie@correo.com")).thenReturn(Optional.empty());

        ValidacionException e = assertThrows(ValidacionException.class, () -> servicio.restablecer(
                new RestablecerContrasenaRequest("nadie@correo.com", "482913", NUEVA, NUEVA)));

        assertEquals(List.of(RecuperacionContrasenaService.MENSAJE_CODIGO_NO_VALIDO), e.getErrores());
    }

    @Test
    void siLaNuevaEsIgualALaActualElCodigoSigueSirviendo() {
        when(usuarioRepository.findByEmailIgnoreCase("daniel@correo.com")).thenReturn(Optional.of(usuario));
        CodigoRecuperacion registro = codigoVigente("482913");
        when(codigoRepository.findById(1)).thenReturn(Optional.of(registro));
        when(contrasenaService.ponerNueva(usuario, NUEVA))
                .thenThrow(new ValidacionException(List.of(ContrasenaService.MENSAJE_IGUAL_A_LA_ACTUAL)));

        assertThrows(ValidacionException.class, () -> servicio.restablecer(conCodigo("482913")));

        verify(codigoRepository, never()).delete(any());
    }

    @Test
    void validaElCodigoYLaContrasenaNuevaAntesDeBuscarNada() {
        ValidacionException e = assertThrows(ValidacionException.class, () -> servicio.restablecer(
                new RestablecerContrasenaRequest("daniel@correo.com", "12", "corta", "otra")));

        assertEquals(List.of(RecuperacionContrasenaService.MENSAJE_FALTA_CODIGO, ReglaContrasena.MENSAJE,
                ContrasenaService.MENSAJE_NO_COINCIDEN), e.getErrores());
        verifyNoInteractions(usuarioRepository, codigoRepository);
    }

    @Test
    void elCodigoGeneradoSiempreTieneSeisDigitos() {
        for (int i = 0; i < 200; i++) {
            assertTrue(RecuperacionContrasenaService.nuevoCodigo().matches("\\d{6}"));
        }
    }

    private static String contains(String texto) {
        return org.mockito.ArgumentMatchers.contains(texto);
    }
}
