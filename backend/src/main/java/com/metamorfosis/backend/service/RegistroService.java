package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.Auditar;
import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.dto.RegistrarCumplimientoRequest;
import com.metamorfosis.backend.dto.RegistroResponse;
import com.metamorfosis.backend.exception.MetaNoEncontradaException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Registro;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.RegistroRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** HU5: registrar el cumplimiento de una meta en una fecha determinada. */
@Service
public class RegistroService {

    private final RegistroRepository registroRepository;
    private final MetaRepository metaRepository;
    private final UsuarioRepository usuarioRepository;

    public RegistroService(RegistroRepository registroRepository,
                            MetaRepository metaRepository,
                            UsuarioRepository usuarioRepository) {
        this.registroRepository = registroRepository;
        this.metaRepository = metaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Registra si el usuario cumplió la meta en la fecha indicada. Si ya existía un
     * registro para esa meta, ese usuario y esa fecha, actualiza su cumplimiento en vez
     * de duplicarlo (un registro por usuario, meta y día).
     */
    @Auditar(accion = AccionLog.REGISTRAR_CUMPLIMIENTO, entidad = EntidadLog.META)
    @Transactional
    public RegistroResponse registrarCumplimiento(Integer codigoMeta,
                                                   RegistrarCumplimientoRequest solicitud,
                                                   Integer codigoUsuario) {
        validar(solicitud);

        Meta meta = metaRepository.findById(codigoMeta)
                .orElseThrow(() -> new MetaNoEncontradaException(codigoMeta));

        if (meta.getEstado() == EstadoMeta.CUMPLIDA) {
            throw new ValidacionException(List.of("Una meta cumplida no admite nuevos registros"));
        }
        if (!metaRepository.esParticipanteAceptado(codigoMeta, codigoUsuario)) {
            throw new PermisoDenegadoException(
                    "Solo un participante de la meta puede registrar su cumplimiento");
        }

        Usuario usuario = usuarioRepository.findById(codigoUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(codigoUsuario));

        Registro registro = registroRepository
                .findByMeta_CodigoMetaAndUsuario_CodigoUsuarioAndFechaRegistro(
                        codigoMeta, codigoUsuario, solicitud.fechaFinalizacion())
                .orElseGet(() -> new Registro(meta, usuario, solicitud.fechaFinalizacion()));

        registro.actualizarCumplimiento(solicitud.cumplimiento());

        // Para el log: qué pasó exactamente (se marcó o se quitó), en qué fecha y en qué meta.
        ContextoAuditoria.agregar("accion", Boolean.TRUE.equals(solicitud.cumplimiento()) ? "marcado" : "quitado");
        ContextoAuditoria.agregar("fechaFinalizacion", solicitud.fechaFinalizacion());
        ContextoAuditoria.agregar("cumplimiento", solicitud.cumplimiento());
        ContextoAuditoria.agregar("codigoMeta", codigoMeta);

        return RegistroResponse.desde(registroRepository.save(registro));
    }

    private void validar(RegistrarCumplimientoRequest s) {
        List<String> errores = new ArrayList<>();
        if (s == null || s.fechaFinalizacion() == null) {
            errores.add("La fecha de finalización es obligatoria");
        } else if (s.fechaFinalizacion().isAfter(LocalDate.now())) {
            errores.add("La fecha de finalización no puede ser una fecha futura");
        }
        if (s == null || s.cumplimiento() == null) {
            errores.add("El cumplimiento es obligatorio");
        }

        if (!errores.isEmpty()) throw new ValidacionException(errores);
    }
}
