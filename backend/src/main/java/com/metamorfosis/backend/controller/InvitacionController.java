package com.metamorfosis.backend.controller;

import com.metamorfosis.backend.dto.InvitacionCorreoResponse;
import com.metamorfosis.backend.dto.InvitacionEnviadaResponse;
import com.metamorfosis.backend.dto.InvitacionRespondidaResponse;
import com.metamorfosis.backend.dto.InvitacionResponse;
import com.metamorfosis.backend.dto.InvitarRequest;
import com.metamorfosis.backend.dto.ResponderInvitacionRequest;
import com.metamorfosis.backend.dto.TokenRequest;
import com.metamorfosis.backend.service.InvitacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** HU13 y HU14: invitaciones a metas compartidas. */
@RestController
public class InvitacionController {

    private final InvitacionService invitacionService;

    public InvitacionController(InvitacionService invitacionService) {
        this.invitacionService = invitacionService;
    }

    /** HU13: invita a otra persona a la meta (solo su creador). */
    @PostMapping("/api/metas/{codigoMeta}/invitaciones")
    public ResponseEntity<InvitacionEnviadaResponse> invitar(@PathVariable Integer codigoMeta,
                                                             @RequestBody InvitarRequest pedido,
                                                             @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invitacionService.invitar(codigoMeta, pedido, codigoUsuario));
    }

    /** HU14: invitaciones pendientes del usuario. */
    @GetMapping("/api/invitaciones")
    public ResponseEntity<List<InvitacionResponse>> pendientes(@AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(invitacionService.pendientes(codigoUsuario));
    }

    /** HU14: acepta ({"aceptar": true}) o rechaza ({"aceptar": false}) la invitación a la meta. */
    @PatchMapping("/api/invitaciones/{codigoMeta}")
    public ResponseEntity<InvitacionRespondidaResponse> responder(@PathVariable Integer codigoMeta,
                                                                  @RequestBody ResponderInvitacionRequest pedido,
                                                                  @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(invitacionService.responder(codigoMeta, pedido, codigoUsuario));
    }

    /** HU14: datos de la invitación del enlace del correo (público). */
    @GetMapping("/api/invitaciones/correo")
    public ResponseEntity<InvitacionCorreoResponse> consultarPorToken(@RequestParam(required = false) String token) {
        return ResponseEntity.ok(invitacionService.consultarPorToken(token));
    }

    /** HU14: aceptar desde el correo (con la sesión de la cuenta invitada). */
    @PostMapping("/api/invitaciones/correo/aceptar")
    public ResponseEntity<InvitacionRespondidaResponse> aceptarPorToken(@RequestBody TokenRequest pedido,
                                                                        @AuthenticationPrincipal Integer codigoUsuario) {
        return ResponseEntity.ok(invitacionService.aceptarPorToken(pedido == null ? null : pedido.token(), codigoUsuario));
    }

    /** HU14: rechazar desde el correo (público). */
    @PostMapping("/api/invitaciones/correo/rechazar")
    public ResponseEntity<Void> rechazarPorToken(@RequestBody TokenRequest pedido) {
        invitacionService.rechazarPorToken(pedido == null ? null : pedido.token());
        return ResponseEntity.noContent().build();
    }
}
