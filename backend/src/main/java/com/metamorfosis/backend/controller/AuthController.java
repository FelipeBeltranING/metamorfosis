package com.metamorfosis.backend.controller;

import com.metamorfosis.backend.dto.RegistroReuest;
import com.metamorfosis.backend.service.AuthService;
import com.metamorfosis.backend.dto.UsuarioResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
        public ResponseEntity<UsuarioResponse> registrar(@RequestBody RegistroRequest solicitud){
            UsuarioResponse usurioResponse = authService.registrar(solicitud);
            return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);    
    }
}