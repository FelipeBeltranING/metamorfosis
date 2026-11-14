package com.metamorfosis.backend.controller;

import com.metamorfosis.backend.dto.AparienciaRequest;
import com.metamorfosis.backend.dto.AvatarPerfil;
import com.metamorfosis.backend.dto.CuentaResponse;
import com.metamorfosis.backend.dto.EditarCuentaRequest;
import com.metamorfosis.backend.dto.FotoPerfilRequest;
import com.metamorfosis.backend.service.CuentaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU-23: la cuenta del usuario autenticado (el usuario sale del token). */
@RestController
@RequestMapping("/api/usuarios/me")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    /** Nombre, apellido, correo, foto y apariencia (HU19), para el encabezado y Editar perfil. */
    @GetMapping
    public ResponseEntity<CuentaResponse> verCuenta(@AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(cuentaService.verCuenta(codigoUsuario));
    }

    @PutMapping
    public ResponseEntity<CuentaResponse> editarDatos(@RequestBody EditarCuentaRequest pedido,
                                                      @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(cuentaService.editarDatos(codigoUsuario, pedido));
    }

    /** HU19: modo y color de acento de la app. */
    @PutMapping("/apariencia")
    public ResponseEntity<CuentaResponse> cambiarApariencia(@RequestBody AparienciaRequest pedido,
                                                            @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(cuentaService.cambiarApariencia(codigoUsuario, pedido));
    }

    /** HU20: guarda el avatar (gratis; todas las piezas están disponibles). */
    @PutMapping("/avatar")
    public ResponseEntity<CuentaResponse> cambiarAvatar(@RequestBody AvatarPerfil pedido,
                                                        @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(cuentaService.cambiarAvatar(codigoUsuario, pedido));
    }

    @PutMapping("/foto")
    public ResponseEntity<CuentaResponse> cambiarFoto(@RequestBody FotoPerfilRequest pedido,
                                                      @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(cuentaService.cambiarFoto(codigoUsuario, pedido));
    }

    @DeleteMapping("/foto")
    public ResponseEntity<CuentaResponse> quitarFoto(@AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(cuentaService.quitarFoto(codigoUsuario));
    }
}
