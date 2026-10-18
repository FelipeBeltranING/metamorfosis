package com.metamorfosis.backend.service;

import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.LogActividad;
import com.metamorfosis.backend.model.ResultadoLog;
import com.metamorfosis.backend.repository.LogActividadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogService {

    private final LogActividadRepository logRepository;

    public LogService(LogActividadRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * Guarda un log en su PROPIA transacción: así el log de una acción que falló
     * (y cuya transacción hizo rollback) igual queda guardado.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Integer codigoUsuario, AccionLog accion, EntidadLog entidad,
                          Integer codigoEntidad, ResultadoLog resultado, String detalle) {
        logRepository.save(new LogActividad(codigoUsuario, accion, entidad, codigoEntidad, resultado, detalle));
    }
}
