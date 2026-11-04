package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.InsigniaResponse;
import com.metamorfosis.backend.model.CriterioInsignia;
import com.metamorfosis.backend.model.Insignia;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.model.UsuarioInsignia;
import com.metamorfosis.backend.repository.InsigniaRepository;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.UsuarioInsigniaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HU11: caja blanca de InsigniaService.otorgarNuevas y de la condición de cada criterio. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InsigniaServiceTest {

    private static final Integer USUARIO = 1;
    private static final LocalDate HOY = LocalDate.of(2026, 11, 4);

    @Mock private InsigniaRepository insigniaRepository;
    @Mock private UsuarioInsigniaRepository usuarioInsigniaRepository;
    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private InsigniaService insigniaService;

    // El catálogo de la migración V14, en su orden
    private final Insignia primerPaso = insignia(1, "Primer paso", CriterioInsignia.METAS_CUMPLIDAS, null, 1);
    private final Insignia enRacha = insignia(2, "En racha", CriterioInsignia.METAS_CUMPLIDAS, null, 5);
    private final Insignia imparable = insignia(3, "Imparable", CriterioInsignia.METAS_CUMPLIDAS, null, 10);
    private final Insignia corredor = insignia(4, "Corredor de corto plazo", CriterioInsignia.METAS_CUMPLIDAS_PLAZO, Plazo.CORTO, 3);
    private final Insignia vision = insignia(5, "Visión de largo plazo", CriterioInsignia.METAS_CUMPLIDAS_PLAZO, Plazo.LARGO, 1);
    private final Insignia todoterreno = insignia(6, "Todoterreno", CriterioInsignia.TIPOS_DISTINTOS, null, 3);

    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario("Felipe", "Beltran", "felipe@test.com", "hash");
        usuario.setCodigoUsuario(USUARIO);
        when(usuarioRepository.getReferenceById(USUARIO)).thenReturn(usuario);
        when(insigniaRepository.findAllByOrderByCodigoInsigniaAsc())
                .thenReturn(List.of(primerPaso, enRacha, imparable, corredor, vision, todoterreno));
        when(usuarioInsigniaRepository.codigosObtenidos(USUARIO)).thenReturn(List.of());
    }

    @Test
    void sinMetasCumplidasNoOtorgaNada() {
        cumplidas();

        assertTrue(insigniaService.otorgarNuevas(USUARIO, HOY).isEmpty());
        verify(usuarioInsigniaRepository, never()).save(any());
        verify(insigniaRepository, never()).findAllByOrderByCodigoInsigniaAsc();
    }

    @Test
    void laPrimeraMetaDaPrimerPasoConLaFechaDeHoy() {
        cumplidas(fila(Plazo.CORTO, TipoMeta.ACADEMICA));

        List<InsigniaResponse> nuevas = insigniaService.otorgarNuevas(USUARIO, HOY);

        assertEquals(List.of("Primer paso"), nombres(nuevas));
        ArgumentCaptor<UsuarioInsignia> guardada = ArgumentCaptor.forClass(UsuarioInsignia.class);
        verify(usuarioInsigniaRepository).save(guardada.capture());
        assertEquals(HOY, guardada.getValue().getFechaObtenida());
        assertEquals(USUARIO, guardada.getValue().getId().getCodigoUsuario());
        assertEquals(1, guardada.getValue().getId().getCodigoInsignia());
    }

    @Test
    void cuatroMetasNoAlcanzanEnRacha() {
        cumplidas(fila(Plazo.MEDIANO, TipoMeta.ACADEMICA), fila(Plazo.MEDIANO, TipoMeta.ACADEMICA),
                fila(Plazo.MEDIANO, TipoMeta.ACADEMICA), fila(Plazo.MEDIANO, TipoMeta.ACADEMICA));
        yaObtenidas(1);

        assertTrue(insigniaService.otorgarNuevas(USUARIO, HOY).isEmpty());
        verify(usuarioInsigniaRepository, never()).save(any());
    }

    @Test
    void laQuintaMetaDaEnRacha() {
        cumplidas(fila(Plazo.MEDIANO, TipoMeta.ACADEMICA), fila(Plazo.MEDIANO, TipoMeta.ACADEMICA),
                fila(Plazo.MEDIANO, TipoMeta.ACADEMICA), fila(Plazo.MEDIANO, TipoMeta.ACADEMICA),
                fila(Plazo.MEDIANO, TipoMeta.ACADEMICA));
        yaObtenidas(1);

        assertEquals(List.of("En racha"), nombres(insigniaService.otorgarNuevas(USUARIO, HOY)));
    }

    @Test
    void unaInsigniaYaObtenidaNoSeVuelveAGuardar() {
        cumplidas(fila(Plazo.CORTO, TipoMeta.ACADEMICA), fila(Plazo.CORTO, TipoMeta.ACADEMICA));
        yaObtenidas(1);

        assertTrue(insigniaService.otorgarNuevas(USUARIO, HOY).isEmpty());
        verify(usuarioInsigniaRepository, never()).save(any());
    }

    @Test
    void elCriterioDePlazoSoloCuentaEsePlazo() {
        cumplidas(fila(Plazo.CORTO, TipoMeta.ACADEMICA), fila(Plazo.CORTO, TipoMeta.ACADEMICA),
                fila(Plazo.LARGO, TipoMeta.ACADEMICA), fila(Plazo.LARGO, TipoMeta.ACADEMICA),
                fila(Plazo.LARGO, TipoMeta.ACADEMICA));
        yaObtenidas(1, 5);

        // 5 metas cumplidas dan En racha, pero solo 2 son de corto plazo: no hay Corredor
        assertEquals(List.of("En racha"), nombres(insigniaService.otorgarNuevas(USUARIO, HOY)));
    }

    @Test
    void laTerceraDeCortoPlazoDaCorredor() {
        cumplidas(fila(Plazo.CORTO, TipoMeta.ACADEMICA), fila(Plazo.CORTO, TipoMeta.ACADEMICA),
                fila(Plazo.CORTO, TipoMeta.ACADEMICA));
        yaObtenidas(1);

        assertEquals(List.of("Corredor de corto plazo"), nombres(insigniaService.otorgarNuevas(USUARIO, HOY)));
    }

    @Test
    void tiposRepetidosCuentanComoUnSoloTipo() {
        cumplidas(fila(Plazo.MEDIANO, TipoMeta.ACADEMICA), fila(Plazo.MEDIANO, TipoMeta.ACADEMICA),
                fila(Plazo.MEDIANO, TipoMeta.ACADEMICA));
        yaObtenidas(1);

        assertTrue(insigniaService.otorgarNuevas(USUARIO, HOY).isEmpty());
    }

    @Test
    void tresTiposDistintosDanTodoterreno() {
        cumplidas(fila(Plazo.MEDIANO, TipoMeta.ACADEMICA), fila(Plazo.MEDIANO, TipoMeta.FINANCIERA),
                fila(Plazo.MEDIANO, TipoMeta.SALUD_BIENESTAR));
        yaObtenidas(1);

        assertEquals(List.of("Todoterreno"), nombres(insigniaService.otorgarNuevas(USUARIO, HOY)));
    }

    @Test
    void variasALaVezSeGuardanTodasEnElOrdenDelCatalogo() {
        cumplidas(fila(Plazo.LARGO, TipoMeta.PERSONAL));

        List<InsigniaResponse> nuevas = insigniaService.otorgarNuevas(USUARIO, HOY);

        assertEquals(List.of("Primer paso", "Visión de largo plazo"), nombres(nuevas));
        verify(usuarioInsigniaRepository, times(2)).save(any(UsuarioInsignia.class));
    }

    @Test
    void alcanzaComparaContraLaCantidadRequerida() {
        InsigniaService.Conteo dos = InsigniaService.Conteo.desde(List.of(
                fila(Plazo.CORTO, TipoMeta.ACADEMICA), fila(Plazo.LARGO, TipoMeta.FINANCIERA)));

        assertTrue(InsigniaService.alcanza(primerPaso, dos));
        assertFalse(InsigniaService.alcanza(enRacha, dos));
        assertTrue(InsigniaService.alcanza(vision, dos));
        assertFalse(InsigniaService.alcanza(corredor, dos));
        assertFalse(InsigniaService.alcanza(todoterreno, dos));
    }

    // ---- ayudas ----

    private void cumplidas(Object[]... filas) {
        List<Object[]> lista = new ArrayList<>(List.of(filas));
        when(metaRepository.plazosYTiposCumplidos(USUARIO)).thenReturn(lista);
    }

    private void yaObtenidas(Integer... codigos) {
        when(usuarioInsigniaRepository.codigosObtenidos(USUARIO)).thenReturn(List.of(codigos));
    }

    private static Object[] fila(Plazo plazo, TipoMeta tipo) {
        return new Object[] {plazo, tipo};
    }

    private static List<String> nombres(List<InsigniaResponse> insignias) {
        return insignias.stream().map(InsigniaResponse::nombre).toList();
    }

    private static Insignia insignia(int codigo, String nombre, CriterioInsignia criterio, Plazo plazo, int cantidad) {
        Insignia i = new Insignia(nombre, "descripción", "Award", criterio, plazo, cantidad);
        try {
            Field id = Insignia.class.getDeclaredField("codigoInsignia");
            id.setAccessible(true);
            id.set(i, codigo);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return i;
    }
}
