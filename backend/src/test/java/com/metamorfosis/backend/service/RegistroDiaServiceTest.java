package com.metamorfosis.backend.service;

import com.metamorfosis.backend.config.CalendarioUsuario;
import com.metamorfosis.backend.dto.DiaResponse;
import com.metamorfosis.backend.dto.RegistrarDiaRequest;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.RegistroSubMeta;
import com.metamorfosis.backend.model.SubMeta;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.model.TipoObjetivo;
import com.metamorfosis.backend.model.TipoSubMeta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.RegistroSubMetaRepository;
import com.metamorfosis.backend.repository.SubMetaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HU5: caja blanca de RegistroDiaService (registrar el día de un hábito). */
@ExtendWith(MockitoExtension.class)
class RegistroDiaServiceTest {

    private static final Integer USUARIO = 1;
    private static final Integer CODIGO_META = 10;
    /** Sábado 14 de noviembre de 2026. */
    private static final LocalDate HOY = LocalDate.of(2026, 11, 14);

    @Mock private SubMetaRepository subMetaRepository;
    @Mock private RegistroSubMetaRepository registroRepository;
    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    private RegistroDiaService servicio;
    private Meta meta;

    @BeforeEach
    void setUp() throws Exception {
        CalendarioUsuario calendario = new CalendarioUsuario(
                Clock.fixed(HOY.atStartOfDay(ZoneId.of("America/Bogota")).toInstant(), ZoneId.of("America/Bogota")));
        servicio = new RegistroDiaService(subMetaRepository, registroRepository, metaRepository,
                usuarioRepository, calendario);
        Usuario creador = new Usuario("Felipe", "Beltrán", "felipe@test.com", "hash");
        creador.setCodigoUsuario(USUARIO);
        meta = new Meta("Leer 20 libros", Plazo.MEDIANO, TipoMeta.PERSONAL);
        meta.crear(creador);
        Field id = Meta.class.getDeclaredField("codigoMeta");
        id.setAccessible(true);
        id.set(meta, CODIGO_META);
    }

    private SubMeta paso(TipoSubMeta tipo, int dias) {
        SubMeta s = new SubMeta(meta, tipo, HOY.minusDays(10));
        if (tipo == TipoSubMeta.MEDIBLE) {
            s.actualizar("Páginas", null, "págs", new BigDecimal("15"), TipoObjetivo.AL_MENOS, dias, null);
        } else {
            s.actualizar("Leer antes de dormir", null, null, null, null, dias, null);
        }
        return s;
    }

    private void participa(SubMeta s) {
        when(subMetaRepository.findById(5)).thenReturn(Optional.of(s));
        when(metaRepository.esParticipanteAceptado(CODIGO_META, USUARIO)).thenReturn(true);
    }

    @Test
    void registrarMedibleDiceSiCumpleElObjetivo() {
        participa(paso(TipoSubMeta.MEDIBLE, SubMeta.TODOS_LOS_DIAS));
        when(registroRepository.findBySubMetaCodigoSubMetaAndUsuarioCodigoUsuarioAndFecha(5, USUARIO, HOY))
                .thenReturn(Optional.empty());

        DiaResponse r = servicio.registrar(5, HOY, new RegistrarDiaRequest(new BigDecimal("20")), USUARIO);

        assertTrue(r.cumplido());
        verify(registroRepository).save(any(RegistroSubMeta.class));
    }

    @Test
    void unHitoNoSeRegistraPorDia() {
        SubMeta hito = new SubMeta(meta, TipoSubMeta.HITO, HOY.minusDays(10));
        hito.actualizar("Terminar el libro 1", null, null, null, null, 0, null);
        participa(hito);

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> servicio.registrar(5, HOY, new RegistrarDiaRequest(BigDecimal.ONE), USUARIO));
        assertEquals(List.of("Un hito no se registra por día: se marca como logrado"), e.getErrores());
        verify(registroRepository, never()).save(any());
    }

    @Test
    void noSeRegistranDiasFuturos() {
        participa(paso(TipoSubMeta.SI_NO, SubMeta.TODOS_LOS_DIAS));

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> servicio.registrar(5, HOY.plusDays(1), new RegistrarDiaRequest(BigDecimal.ONE), USUARIO));
        assertEquals(List.of("No puedes registrar días que aún no llegan"), e.getErrores());
    }

    @Test
    void noSeRegistraUnDiaQueNoToca() {
        participa(paso(TipoSubMeta.SI_NO, SubMeta.bitDe(DayOfWeek.MONDAY)));

        ValidacionException e = assertThrows(ValidacionException.class,
                () -> servicio.registrar(5, HOY, new RegistrarDiaRequest(BigDecimal.ONE), USUARIO));
        assertEquals(List.of("Esta sub-meta no toca ese día"), e.getErrores());
    }

    @Test
    void enSiNoSoloSeAceptaUnoOCero() {
        participa(paso(TipoSubMeta.SI_NO, SubMeta.TODOS_LOS_DIAS));

        assertThrows(ValidacionException.class,
                () -> servicio.registrar(5, HOY, new RegistrarDiaRequest(new BigDecimal("2")), USUARIO));
    }

    @Test
    void quienNoParticipaNoRegistra() {
        when(subMetaRepository.findById(5)).thenReturn(Optional.of(paso(TipoSubMeta.SI_NO, SubMeta.TODOS_LOS_DIAS)));
        when(metaRepository.esParticipanteAceptado(CODIGO_META, USUARIO)).thenReturn(false);

        assertThrows(PermisoDenegadoException.class,
                () -> servicio.registrar(5, HOY, new RegistrarDiaRequest(BigDecimal.ONE), USUARIO));
    }
}
