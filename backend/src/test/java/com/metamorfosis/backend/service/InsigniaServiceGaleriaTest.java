package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.dto.GaleriaInsigniasResponse;
import com.metamorfosis.backend.dto.GaleriaInsigniasResponse.InsigniaGaleria;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** HU12: caja blanca de InsigniaService.verGaleria e InsigniaService.avance. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InsigniaServiceGaleriaTest {

    private static final Integer USUARIO = 1;

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

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Felipe", "Beltran", "felipe@test.com", "hash");
        usuario.setCodigoUsuario(USUARIO);
        when(insigniaRepository.findAllByOrderByCodigoInsigniaAsc())
                .thenReturn(List.of(primerPaso, enRacha, imparable, corredor, vision, todoterreno));
        cumplidas();
        ganadas();
    }

    @Test
    void sinMetasCumplidasTodasPendientesEnCero() {
        GaleriaInsigniasResponse g = insigniaService.verGaleria(USUARIO);

        assertEquals(0, g.ganadas());
        assertEquals(6, g.total());
        assertTrue(g.insignias().stream().noneMatch(InsigniaGaleria::obtenida));
        assertTrue(g.insignias().stream().allMatch(i -> i.avance() == 0 && i.fechaObtenida() == null));
    }

    @Test
    void criterioMetasCumplidasCuentaElTotal() {
        cumplidas(fila(Plazo.CORTO, TipoMeta.ACADEMICA), fila(Plazo.CORTO, TipoMeta.SALUD_BIENESTAR),
                fila(Plazo.LARGO, TipoMeta.FINANCIERA));

        assertEquals(3, porNombre(insigniaService.verGaleria(USUARIO), "En racha").avance());
    }

    @Test
    void criterioPorPlazoCuentaSoloElPlazoPedido() {
        cumplidas(fila(Plazo.CORTO, TipoMeta.ACADEMICA), fila(Plazo.CORTO, TipoMeta.SALUD_BIENESTAR),
                fila(Plazo.LARGO, TipoMeta.FINANCIERA));

        GaleriaInsigniasResponse g = insigniaService.verGaleria(USUARIO);

        assertEquals(2, porNombre(g, "Corredor de corto plazo").avance());
        assertEquals(1, porNombre(g, "Visión de largo plazo").avance());
    }

    @Test
    void criterioTiposDistintosCuentaCadaTipoUnaVez() {
        cumplidas(fila(Plazo.CORTO, TipoMeta.ACADEMICA), fila(Plazo.MEDIANO, TipoMeta.ACADEMICA),
                fila(Plazo.LARGO, TipoMeta.SALUD_BIENESTAR));

        assertEquals(2, porNombre(insigniaService.verGaleria(USUARIO), "Todoterreno").avance());
    }

    @Test
    void elAvanceNoPasaDeLoQuePideLaInsignia() {
        Object[][] doce = new Object[12][];
        Arrays.fill(doce, fila(Plazo.MEDIANO, TipoMeta.PERSONAL));
        cumplidas(doce);

        GaleriaInsigniasResponse g = insigniaService.verGaleria(USUARIO);

        assertEquals(10, porNombre(g, "Imparable").avance());
        assertEquals(5, porNombre(g, "En racha").avance());
    }

    @Test
    void insigniaGanadaTraeSuFechaYElAvanceCompleto() {
        cumplidas(fila(Plazo.CORTO, TipoMeta.ACADEMICA));
        ganadas(new UsuarioInsignia(usuario, primerPaso, LocalDate.of(2026, 11, 1)));

        InsigniaGaleria primer = porNombre(insigniaService.verGaleria(USUARIO), "Primer paso");

        assertTrue(primer.obtenida());
        assertEquals(LocalDate.of(2026, 11, 1), primer.fechaObtenida());
        assertEquals(1, primer.avance());
        assertEquals(1, primer.cantidadRequerida());
    }

    @Test
    void insigniaGanadaSeConservaAunqueYaNoHayaMetasCumplidas() {
        ganadas(new UsuarioInsignia(usuario, primerPaso, LocalDate.of(2026, 11, 1)));

        InsigniaGaleria primer = porNombre(insigniaService.verGaleria(USUARIO), "Primer paso");

        assertTrue(primer.obtenida());
        assertEquals(1, primer.avance());
    }

    @Test
    void sigueElOrdenDelCatalogoYCuentaLasGanadas() {
        cumplidas(fila(Plazo.LARGO, TipoMeta.FINANCIERA));
        // la consulta las trae de la más reciente a la más antigua; la galería usa el orden del catálogo
        ganadas(new UsuarioInsignia(usuario, vision, LocalDate.of(2026, 11, 3)),
                new UsuarioInsignia(usuario, primerPaso, LocalDate.of(2026, 11, 3)));

        try (MockedStatic<ContextoAuditoria> contexto = mockStatic(ContextoAuditoria.class)) {
            GaleriaInsigniasResponse g = insigniaService.verGaleria(USUARIO);

            assertEquals(List.of("Primer paso", "En racha", "Imparable", "Corredor de corto plazo",
                    "Visión de largo plazo", "Todoterreno"), g.insignias().stream().map(InsigniaGaleria::nombre).toList());
            assertEquals(2, g.ganadas());
            assertEquals(6, g.total());
            contexto.verify(() -> ContextoAuditoria.agregar("ganadas", 2));
        }
    }

    @Test
    void avanceDirectoPorCriterio() {
        InsigniaService.Conteo conteo = InsigniaService.Conteo.desde(List.of(
                fila(Plazo.CORTO, TipoMeta.ACADEMICA), fila(Plazo.CORTO, TipoMeta.ACADEMICA),
                fila(Plazo.MEDIANO, TipoMeta.FINANCIERA), fila(Plazo.LARGO, TipoMeta.SALUD_BIENESTAR)));

        assertEquals(1, InsigniaService.avance(primerPaso, conteo));
        assertEquals(4, InsigniaService.avance(enRacha, conteo));
        assertEquals(2, InsigniaService.avance(corredor, conteo));
        assertEquals(1, InsigniaService.avance(vision, conteo));
        assertEquals(3, InsigniaService.avance(todoterreno, conteo));
    }

    // ---- ayudas ----

    private void cumplidas(Object[]... filas) {
        List<Object[]> lista = new ArrayList<>(List.of(filas));
        when(metaRepository.plazosYTiposCumplidos(USUARIO)).thenReturn(lista);
    }

    private void ganadas(UsuarioInsignia... filas) {
        when(usuarioInsigniaRepository.insigniasDelUsuario(USUARIO)).thenReturn(List.of(filas));
    }

    private static Object[] fila(Plazo plazo, TipoMeta tipo) {
        return new Object[] {plazo, tipo};
    }

    private static InsigniaGaleria porNombre(GaleriaInsigniasResponse g, String nombre) {
        return g.insignias().stream().filter(i -> i.nombre().equals(nombre)).findFirst().orElseThrow();
    }

    private static Insignia insignia(int codigo, String nombre, CriterioInsignia criterio, Plazo plazo, int cantidad) {
        Insignia i = new Insignia(nombre, "descripción", "Award", criterio, plazo, cantidad);
        try {
            Field f = Insignia.class.getDeclaredField("codigoInsignia");
            f.setAccessible(true);
            f.set(i, codigo);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return i;
    }
}
