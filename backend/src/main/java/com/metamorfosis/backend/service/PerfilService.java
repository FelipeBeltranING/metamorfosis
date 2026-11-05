package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.Auditar;
import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.dto.PerfilResponse;
import com.metamorfosis.backend.dto.PerfilResponse.InsigniaObtenida;
import com.metamorfosis.backend.dto.PerfilResponse.ResumenMetas;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.InsigniaRepository;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.UsuarioInsigniaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** HU22: arma Mi perfil con los datos del usuario, el resumen de metas, las insignias y las últimas cumplidas. */
@Service
public class PerfilService {

    static final int ULTIMAS_CUMPLIDAS = 3;

    /** De la cumplida más reciente a la más antigua; si dos tienen la misma fecha, la de código mayor primero. */
    private static final Comparator<MetaResponse> MAS_RECIENTE_PRIMERO = Comparator
            .comparing(MetaResponse::fechaCumplida, Comparator.nullsFirst(Comparator.<LocalDate>naturalOrder()))
            .thenComparing(MetaResponse::codigoMeta)
            .reversed();

    private final UsuarioRepository usuarioRepository;
    private final MetaRepository metaRepository;
    private final UsuarioInsigniaRepository usuarioInsigniaRepository;
    private final InsigniaRepository insigniaRepository;

    public PerfilService(UsuarioRepository usuarioRepository, MetaRepository metaRepository,
                         UsuarioInsigniaRepository usuarioInsigniaRepository,
                         InsigniaRepository insigniaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.metaRepository = metaRepository;
        this.usuarioInsigniaRepository = usuarioInsigniaRepository;
        this.insigniaRepository = insigniaRepository;
    }

    @Auditar(accion = AccionLog.VER_PERFIL, entidad = EntidadLog.USUARIO)
    @Transactional(readOnly = true)
    public PerfilResponse verMiPerfil(Integer codigoUsuario) {
        Usuario usuario = usuarioRepository.findById(codigoUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(codigoUsuario));

        // 1. Resumen: las metas en las que participa con invitación aceptada (igual que el reporte, HU7).
        List<Meta> metas = metaRepository.findByParticipante(codigoUsuario);
        long total = metas.size();
        long cumplidas = metas.stream().filter(m -> m.getEstado() == EstadoMeta.CUMPLIDA).count();
        ResumenMetas resumen = new ResumenMetas(total, total - cumplidas, cumplidas,
                ReporteService.porcentaje(cumplidas, total));

        // 2. Últimas cumplidas: máximo tres, de la más reciente a la más antigua.
        List<MetaResponse> ultimasCumplidas = metas.stream()
                .filter(m -> m.getEstado() == EstadoMeta.CUMPLIDA)
                .map(MetaResponse::desde)
                .sorted(MAS_RECIENTE_PRIMERO)
                .limit(ULTIMAS_CUMPLIDAS)
                .toList();

        // 3. Insignias ganadas (HU11), en el orden de la consulta: la más reciente primero.
        List<InsigniaObtenida> insignias = usuarioInsigniaRepository.insigniasDelUsuario(codigoUsuario).stream()
                .map(InsigniaObtenida::desde)
                .toList();

        ContextoAuditoria.agregar("totalMetas", total);
        ContextoAuditoria.agregar("metasCumplidas", cumplidas);
        ContextoAuditoria.agregar("insignias", insignias.size());

        return new PerfilResponse(usuario.getNombre(), usuario.getApellido(), usuario.getEmail(),
                usuario.getFechaRegistro(), resumen, insignias, insigniaRepository.count(), ultimasCumplidas);
    }
}
