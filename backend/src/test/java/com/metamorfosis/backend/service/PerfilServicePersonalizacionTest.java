package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.PerfilPublicoRequest;
import com.metamorfosis.backend.dto.PerfilPublicoResponse;
import com.metamorfosis.backend.dto.PerfilResponse;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.Portada;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.InsigniaRepository;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.UsuarioInsigniaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** HU19: caja blanca de PerfilService.editarPerfilPublico y de lo que muestran los perfiles. */
@ExtendWith(MockitoExtension.class)
class PerfilServicePersonalizacionTest {

    private static final Integer DANIEL = 1;
    private static final Integer LAURA = 2;

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private MetaRepository metaRepository;
    @Mock private UsuarioInsigniaRepository usuarioInsigniaRepository;
    @Mock private InsigniaRepository insigniaRepository;

    @InjectMocks
    private PerfilService perfilService;

    private Usuario daniel;

    @BeforeEach
    void preparar() {
        daniel = new Usuario("Daniel", "Santos", "daniel@correo.com", "hash");
        daniel.setCodigoUsuario(DANIEL);
        daniel.setFechaRegistro(LocalDate.of(2026, 9, 20));
    }

    private void perfilCompletoSeArma() {
        when(usuarioRepository.findById(DANIEL)).thenReturn(Optional.of(daniel));
        when(metaRepository.findByParticipante(DANIEL)).thenReturn(List.of());
        when(usuarioInsigniaRepository.insigniasDelUsuario(DANIEL)).thenReturn(List.of());
    }

    @Test
    void guardaLaFraseLimpiaYLaPortada() {
        perfilCompletoSeArma();

        PerfilResponse r = perfilService.editarPerfilPublico(
                new PerfilPublicoRequest("  Un   libro a la vez ", "aurora", null, null), DANIEL);

        assertEquals("Un libro a la vez", daniel.getFrase());
        assertEquals(Portada.AURORA, daniel.getPortada());
        assertEquals("Un libro a la vez", r.personalizacion().frase());
        assertEquals(Portada.AURORA, r.personalizacion().portada());
        verify(usuarioRepository).save(daniel);
    }

    @Test
    void fraseVaciaSeBorraYSinPortadaQuedaNinguna() {
        daniel.setFrase("Antes");
        daniel.setPortada(Portada.BOSQUE);
        perfilCompletoSeArma();

        perfilService.editarPerfilPublico(new PerfilPublicoRequest("   ", null, null, null), DANIEL);

        assertNull(daniel.getFrase());
        assertEquals(Portada.NINGUNA, daniel.getPortada());
    }

    @Test
    void fraseMuyLargaYPortadaInvalidaNoSeGuardan() {
        when(usuarioRepository.findById(DANIEL)).thenReturn(Optional.of(daniel));

        ValidacionException e = assertThrows(ValidacionException.class, () -> perfilService.editarPerfilPublico(
                new PerfilPublicoRequest("x".repeat(101), "ARCOIRIS", null, null), DANIEL));

        assertEquals(List.of("La frase puede tener hasta 100 caracteres", "Elige una portada válida"), e.getErrores());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void sinDatosEsInvalido() {
        when(usuarioRepository.findById(DANIEL)).thenReturn(Optional.of(daniel));

        assertThrows(ValidacionException.class, () -> perfilService.editarPerfilPublico(null, DANIEL));
    }

    @Test
    void unPerfilPrivadoMuestraLaFraseYLaPortadaPeroNadaMas() {
        Usuario laura = new Usuario("Laura", "Gómez", "laura@correo.com", "hash");
        laura.setCodigoUsuario(LAURA);
        laura.setPerfilPrivado(true);
        laura.setFrase("Paso a paso");
        laura.setPortada(Portada.OCEANO);
        when(usuarioRepository.findById(LAURA)).thenReturn(Optional.of(laura));

        PerfilPublicoResponse r = perfilService.verPerfilPublico(LAURA, DANIEL);

        assertTrue(r.privado());
        assertEquals("Paso a paso", r.personalizacion().frase());
        assertEquals(Portada.OCEANO, r.personalizacion().portada());
        assertNull(r.personalizacion().insigniaDestacada());
        assertTrue(r.personalizacion().metasDestacadas().isEmpty());
        verifyNoInteractions(metaRepository);
    }

    @Test
    void limpiarFrase() {
        assertEquals("Hola mundo", PerfilService.limpiarFrase(" Hola \n mundo "));
        assertNull(PerfilService.limpiarFrase(null));
        assertNull(PerfilService.limpiarFrase("  "));
    }

    private static com.metamorfosis.backend.model.Insignia insignia(int codigo, String nombre) throws Exception {
        com.metamorfosis.backend.model.Insignia i = new com.metamorfosis.backend.model.Insignia(nombre, "descripción",
                "Flame", com.metamorfosis.backend.model.CriterioInsignia.METAS_CUMPLIDAS, null, 1);
        java.lang.reflect.Field f = com.metamorfosis.backend.model.Insignia.class.getDeclaredField("codigoInsignia");
        f.setAccessible(true);
        f.set(i, codigo);
        return i;
    }

    @Test
    void destacaUnaInsigniaQueGanoYSeMuestraEnLosPerfiles() throws Exception {
        when(usuarioRepository.findById(DANIEL)).thenReturn(Optional.of(daniel));
        when(metaRepository.findByParticipante(DANIEL)).thenReturn(List.of());
        when(usuarioInsigniaRepository.codigosObtenidos(DANIEL)).thenReturn(List.of(1, 3));
        when(usuarioInsigniaRepository.insigniasDelUsuario(DANIEL)).thenReturn(List.of(
                new com.metamorfosis.backend.model.UsuarioInsignia(daniel, insignia(3, "En racha"), LocalDate.of(2026, 10, 30)),
                new com.metamorfosis.backend.model.UsuarioInsignia(daniel, insignia(1, "Primer paso"), LocalDate.of(2026, 10, 10))));

        PerfilResponse r = perfilService.editarPerfilPublico(new PerfilPublicoRequest(null, "NINGUNA", 3, null), DANIEL);

        assertEquals(3, daniel.getCodigoInsigniaDestacada());
        assertEquals("En racha", r.personalizacion().insigniaDestacada().nombre());

        PerfilPublicoResponse publico = perfilService.verPerfilPublico(DANIEL, LAURA);
        assertEquals("En racha", publico.personalizacion().insigniaDestacada().nombre());
    }

    @Test
    void noSeDestacaUnaInsigniaQueNoGano() {
        when(usuarioRepository.findById(DANIEL)).thenReturn(Optional.of(daniel));
        when(usuarioInsigniaRepository.codigosObtenidos(DANIEL)).thenReturn(List.of(1));

        ValidacionException e = assertThrows(ValidacionException.class, () -> perfilService.editarPerfilPublico(
                new PerfilPublicoRequest(null, null, 5, null), DANIEL));

        assertEquals("Elige una insignia que hayas ganado", e.getErrores().get(0));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void sinInsigniaDestacadaSeQuita() {
        daniel.setCodigoInsigniaDestacada(1);
        perfilCompletoSeArma();

        PerfilResponse r = perfilService.editarPerfilPublico(new PerfilPublicoRequest(null, null, null, null), DANIEL);

        assertNull(daniel.getCodigoInsigniaDestacada());
        assertNull(r.personalizacion().insigniaDestacada());
    }

    private com.metamorfosis.backend.model.Meta meta(int codigo, LocalDate creada) throws Exception {
        com.metamorfosis.backend.model.Meta m = new com.metamorfosis.backend.model.Meta("Meta " + codigo,
                com.metamorfosis.backend.model.Plazo.CORTO, com.metamorfosis.backend.model.TipoMeta.PERSONAL);
        m.crear(daniel);
        java.lang.reflect.Field f = com.metamorfosis.backend.model.Meta.class.getDeclaredField("codigoMeta");
        f.setAccessible(true);
        f.set(m, codigo);
        java.lang.reflect.Field c = com.metamorfosis.backend.model.Meta.class.getDeclaredField("fechaCreacion");
        c.setAccessible(true);
        c.set(m, creada);
        return m;
    }

    @Test
    void destacaHastaTresMetasYSeMuestranLaMasRecientePrimero() throws Exception {
        var m10 = meta(10, LocalDate.of(2026, 9, 1));
        var m11 = meta(11, LocalDate.of(2026, 10, 1));
        var m12 = meta(12, LocalDate.of(2026, 8, 1));
        when(usuarioRepository.findById(DANIEL)).thenReturn(Optional.of(daniel));
        when(metaRepository.findByParticipante(DANIEL)).thenReturn(List.of(m10, m11, m12));
        when(usuarioInsigniaRepository.insigniasDelUsuario(DANIEL)).thenReturn(List.of());

        PerfilResponse r = perfilService.editarPerfilPublico(
                new PerfilPublicoRequest(null, null, null, List.of(10, 11, 11)), DANIEL);

        assertTrue(m10.isDestacadaPara(DANIEL));
        assertTrue(m11.isDestacadaPara(DANIEL));
        assertFalse(m12.isDestacadaPara(DANIEL));
        assertEquals(List.of(11, 10), r.personalizacion().metasDestacadas().stream()
                .map(com.metamorfosis.backend.dto.MetaResponse::codigoMeta).toList());
        assertTrue(r.personalizacion().metasDestacadas().get(0).participantes().isEmpty());
        verify(metaRepository).saveAll(List.of(m10, m11, m12));
    }

    @Test
    void masDeTresMetasNoSePueden() {
        when(usuarioRepository.findById(DANIEL)).thenReturn(Optional.of(daniel));

        ValidacionException e = assertThrows(ValidacionException.class, () -> perfilService.editarPerfilPublico(
                new PerfilPublicoRequest(null, null, null, List.of(1, 2, 3, 4)), DANIEL));

        assertEquals("Puedes destacar hasta 3 metas", e.getErrores().get(0));
        verify(metaRepository, never()).saveAll(any());
    }

    @Test
    void noSeDestacaUnaMetaAjenaNiUnaOculta() throws Exception {
        var oculta = meta(10, LocalDate.of(2026, 9, 1));
        oculta.cambiarVisibilidad(DANIEL, true);
        when(usuarioRepository.findById(DANIEL)).thenReturn(Optional.of(daniel));
        when(metaRepository.findByParticipante(DANIEL)).thenReturn(List.of(oculta));

        ValidacionException e1 = assertThrows(ValidacionException.class, () -> perfilService.editarPerfilPublico(
                new PerfilPublicoRequest(null, null, null, List.of(10)), DANIEL));
        ValidacionException e2 = assertThrows(ValidacionException.class, () -> perfilService.editarPerfilPublico(
                new PerfilPublicoRequest(null, null, null, List.of(99)), DANIEL));

        assertEquals("Solo puedes destacar metas tuyas que se vean en tu perfil", e1.getErrores().get(0));
        assertEquals("Solo puedes destacar metas tuyas que se vean en tu perfil", e2.getErrores().get(0));
    }

    @Test
    void alOcultarUnaMetaDejaDeEstarDestacada() throws Exception {
        var m = meta(10, LocalDate.of(2026, 9, 1));
        m.cambiarDestacada(DANIEL, true);

        m.cambiarVisibilidad(DANIEL, true);

        assertFalse(m.isDestacadaPara(DANIEL));
    }
}
