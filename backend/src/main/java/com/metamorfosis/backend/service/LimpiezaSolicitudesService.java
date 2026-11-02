package com.metamorfosis.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/** HU8: cada hora borra las solicitudes de registro vencidas y los bloqueos que ya terminaron. */
@Service
public class LimpiezaSolicitudesService {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaSolicitudesService.class);

    private final SolicitudRegistroService solicitudRegistroService;

    public LimpiezaSolicitudesService(SolicitudRegistroService solicitudRegistroService) {
        this.solicitudRegistroService = solicitudRegistroService;
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    public void limpiar() {
        int borradas = solicitudRegistroService.limpiarVencidas();
        if (borradas > 0) log.info("Solicitudes de registro vencidas borradas: {}", borradas);
    }
}
