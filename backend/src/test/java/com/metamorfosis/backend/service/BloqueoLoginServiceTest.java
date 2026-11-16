package com.metamorfosis.backend.service;

import com.metamorfosis.backend.model.IntentoLogin;
import com.metamorfosis.backend.repository.IntentoLoginRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** HU9: caja blanca de BloqueoLoginService, el bloqueo tras 5 intentos fallidos (sección 16.7). */
@ExtendWith(MockitoExtension.class)
class BloqueoLoginServiceTest {

    private static final String EMAIL = "ana@test.com";
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 10, 0);

    @Mock private IntentoLoginRepository repositorio;

    private BloqueoLoginService servicio;

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
        servicio = new BloqueoLoginService(repositorio, reloj);
    }

    private IntentoLogin conIntentos(int intentos, LocalDateTime bloqueadoHasta) {
        IntentoLogin i = new IntentoLogin(EMAIL);
        i.setIntentos(intentos);
        i.setBloqueadoHasta(bloqueadoHasta);
        when(repositorio.findById(EMAIL)).thenReturn(Optional.of(i));
        return i;
    }

    @Test
    void sinHistorialNoEstaBloqueado() {
        when(repositorio.findById(EMAIL)).thenReturn(Optional.empty());
        assertEquals(0, servicio.segundosBloqueado(EMAIL));
    }

    @Test
    void elPrimerFalloSeCuentaSinBloquear() {
        when(repositorio.findById(EMAIL)).thenReturn(Optional.empty());

        servicio.registrarFallo(EMAIL);

        ArgumentCaptor<IntentoLogin> guardado = ArgumentCaptor.forClass(IntentoLogin.class);
        verify(repositorio).save(guardado.capture());
        assertEquals(1, guardado.getValue().getIntentos());
        assertNull(guardado.getValue().getBloqueadoHasta());
    }

    @Test
    void elQuintoFalloBloqueaCincoMinutos() {
        IntentoLogin intento = conIntentos(4, null);

        servicio.registrarFallo(EMAIL);

        assertEquals(5, intento.getIntentos());
        assertEquals(AHORA.plusMinutes(5), intento.getBloqueadoHasta());
        assertEquals(301, servicio.segundosBloqueado(EMAIL));
    }

    @Test
    void unBloqueoVencidoNoCuentaYElConteoEmpiezaDeCero() {
        IntentoLogin intento = conIntentos(5, AHORA.minusSeconds(1));
        assertEquals(0, servicio.segundosBloqueado(EMAIL));

        servicio.registrarFallo(EMAIL);

        assertEquals(1, intento.getIntentos());
        assertNull(intento.getBloqueadoHasta());
    }

    @Test
    void unInicioCorrectoBorraElHistorial() {
        when(repositorio.existsById(EMAIL)).thenReturn(true);
        servicio.limpiar(EMAIL);
        verify(repositorio).deleteById(EMAIL);

        when(repositorio.existsById("otro@test.com")).thenReturn(false);
        servicio.limpiar("otro@test.com");
        verify(repositorio, never()).deleteById("otro@test.com");
    }
}
