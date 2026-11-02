package com.metamorfosis.backend.controller;

import com.metamorfosis.backend.dto.RegistroRequest;
import com.metamorfosis.backend.dto.SolicitudRegistroRequest;
import com.metamorfosis.backend.dto.SolicitudRegistroResponse;
import com.metamorfosis.backend.dto.TokenRequest;
import com.metamorfosis.backend.dto.UsuarioResponse;
import com.metamorfosis.backend.exception.TokenInvalidoException;
import com.metamorfosis.backend.dto.LoginRequest;
import com.metamorfosis.backend.dto.LoginResponse;
import com.metamorfosis.backend.service.AuthService;
import com.metamorfosis.backend.service.SolicitudRegistroService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SolicitudRegistroService solicitudRegistroService;

    public AuthController(AuthService authService, SolicitudRegistroService solicitudRegistroService) {
        this.authService = authService;
        this.solicitudRegistroService = solicitudRegistroService;
    }

    /** HU8 paso 1: envía al email el correo para autorizar el registro (202). */
    @PostMapping("/registro/solicitar")
    public ResponseEntity<SolicitudRegistroResponse> solicitarRegistro(@RequestBody SolicitudRegistroRequest pedido) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(solicitudRegistroService.solicitar(pedido));
    }

    /** HU8 paso 2: comprueba el enlace del correo y devuelve el email autorizado. Solo lee. */
    @GetMapping("/registro/solicitud/{token}")
    public ResponseEntity<SolicitudRegistroResponse> consultarSolicitud(@PathVariable String token) {
        return ResponseEntity.ok(solicitudRegistroService.consultar(token));
    }

    /** HU8: "No fui yo". Invalida el enlace y bloquea el email por 24 h (204). */
    @PostMapping("/registro/rechazar")
    public ResponseEntity<Void> rechazarRegistro(@RequestBody TokenRequest pedido) {
        solicitudRegistroService.rechazar(pedido == null ? null : pedido.token());
        return ResponseEntity.noContent().build();
    }

    /** HU8 paso 3: crea la cuenta; exige el token del correo (201). */
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@RequestBody RegistroRequest solicitud) {
        UsuarioResponse respuesta = authService.registrar(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> iniciarSesion(@RequestBody LoginRequest solicitud) {
        return ResponseEntity.ok(authService.iniciarSesion(solicitud));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> cerrarSesion(
            @RequestHeader(value = "Authorization", required = false) String autorizacion) {
        if (autorizacion == null || !autorizacion.startsWith("Bearer ")) {
            throw new TokenInvalidoException("Falta el token de autorización");
        }
        authService.cerrarSesion(autorizacion.substring("Bearer ".length()).trim());
        return ResponseEntity.noContent().build();
    }
    
}
