package com.metamorfosis.backend.controller;

import com.metamorfosis.backend.dto.PerfilResponse;
import com.metamorfosis.backend.service.PerfilService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final PerfilService perfilService;

    public UsuarioController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    /** HU22: Mi perfil del usuario autenticado (el usuario sale del token). */
    @GetMapping("/me/perfil")
    public ResponseEntity<PerfilResponse> miPerfil(@AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(perfilService.verMiPerfil(codigoUsuario));
    }
}
