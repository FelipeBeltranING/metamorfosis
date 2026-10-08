package com.metamorfosis.backend.controller;

import com.metamorfosis.backend.dto.RegistroRequest;
import com.metamorfosis.backend.dto.UsuarioResponse;
import com.metamorfosis.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest solicitud) {
        UsuarioResponse respuesta = authService.registrar(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}
