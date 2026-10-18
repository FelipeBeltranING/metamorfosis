package com.metamorfosis.backend.service;

import com.metamorfosis.backend.model.LogActividad;
import com.metamorfosis.backend.model.ResultadoLog;
import com.metamorfosis.backend.repository.LogRepository;
import org.springframework.stereotype.Service;

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