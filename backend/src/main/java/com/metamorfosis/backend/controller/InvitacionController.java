package com.metamorfosis.backend.controller;

import com.metamorfosis.backend.dto.InvitacionEnviadaResponse;
import com.metamorfosis.backend.dto.InvitarRequest;
import com.metamorfosis.backend.service.InvitacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
}
