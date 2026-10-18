package com.metamorfosis.backend.service;

import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.LogActividad;
import com.metamorfosis.backend.model.ResultadoLog;
import com.metamorfosis.backend.repository.LogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogService {

    public static final String ACCION_REGISTRO = "REGISTRO";
    public static final String ENTIDAD_USUARIO = "USUARIO";

    private final LogRepository logRepository;

    public LogService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public void registrarExito(Integer usuarioId, String accion, String entidad, Integer entidadId) {
        LogActividad log = construir(usuarioId, accion, entidad, entidadId, ResultadoLog.EXITO, null);
        logRepository.save(log);
    }

    /**
     * Auditoría (@Auditar): guarda el log en su PROPIA transacción, así el log de una acción
     * que falló (y cuya transacción hizo rollback) igual queda guardado.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Integer codigoUsuario, AccionLog accion, EntidadLog entidad,
                          Integer codigoEntidad, ResultadoLog resultado, String detalle) {
        logRepository.save(construir(codigoUsuario, accion.name(), entidad.name(),
                codigoEntidad, resultado, detalle));
    }

    private LogActividad construir(Integer usuarioId, String accion, String entidad, Integer entidadId, ResultadoLog resultado, String detalle) {
        LogActividad log = new LogActividad();
        log.setCodigoUsuario(usuarioId);
        log.setAccion(accion);
        log.setEntidad(entidad);
        log.setCodigoEntidad(entidadId);
        log.setResultado(resultado);
        log.setDetalle(detalle);
        return log;
    }
}