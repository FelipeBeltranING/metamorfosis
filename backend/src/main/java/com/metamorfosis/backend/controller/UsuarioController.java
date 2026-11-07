package com.metamorfosis.backend.controller;

import com.metamorfosis.backend.dto.GaleriaInsigniasResponse;
import com.metamorfosis.backend.dto.PerfilPublicoResponse;
import com.metamorfosis.backend.dto.PerfilResponse;
import com.metamorfosis.backend.dto.PrivacidadRequest;
import com.metamorfosis.backend.dto.PrivacidadResponse;
import com.metamorfosis.backend.dto.UsuarioResumenResponse;
import com.metamorfosis.backend.service.InsigniaService;
import com.metamorfosis.backend.service.PerfilService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final PerfilService perfilService;
    private final InsigniaService insigniaService;

    public UsuarioController(PerfilService perfilService, InsigniaService insigniaService) {
        this.perfilService = perfilService;
        this.insigniaService = insigniaService;
    }

    /** HU22: Mi perfil del usuario autenticado (el usuario sale del token). */
    @GetMapping("/me/perfil")
    public ResponseEntity<PerfilResponse> miPerfil(@AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(perfilService.verMiPerfil(codigoUsuario));
    }

    /** HU12: galería con las seis insignias del catálogo y el avance del usuario. */
    @GetMapping("/me/insignias")
    public ResponseEntity<GaleriaInsigniasResponse> misInsignias(@AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(insigniaService.verGaleria(codigoUsuario));
    }

    /** HU24: busca personas por nombre o apellido (?nombre=, al menos dos letras). */
    @GetMapping
    public ResponseEntity<List<UsuarioResumenResponse>> buscarUsuarios(
            @RequestParam(required = false) String nombre,
            @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(perfilService.buscarUsuarios(nombre, codigoUsuario));
    }

    /** HU24: perfil público de otra persona. La ruta fija /me/perfil (HU22) tiene prioridad. */
    @GetMapping("/{codigoUsuario}/perfil")
    public ResponseEntity<PerfilPublicoResponse> perfilPublico(
            @PathVariable("codigoUsuario") Integer codigoPerfil,
            @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(perfilService.verPerfilPublico(codigoPerfil, codigoUsuario));
    }

    /** HU26: hace privado ({"privado": true}) o público ({"privado": false}) el perfil. */
    @PatchMapping("/me/privacidad")
    public ResponseEntity<PrivacidadResponse> cambiarPrivacidad(@RequestBody PrivacidadRequest solicitud,
                                                                @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(perfilService.cambiarPrivacidad(solicitud, codigoUsuario));
    }
}
