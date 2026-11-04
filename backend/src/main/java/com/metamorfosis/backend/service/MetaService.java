package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.Auditar;
import com.metamorfosis.backend.auditoria.ContextoAuditoria;
import com.metamorfosis.backend.dto.CrearMetaRequest;
import com.metamorfosis.backend.dto.FiltroEstado;
import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.metamorfosis.backend.exception.MetaNoEncontradaException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Plazo;
import com.metamorfosis.backend.model.TipoMeta;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MetaService {

    private static final int NOMBRE_MAX = 200;

    private final MetaRepository metaRepository;
    private final UsuarioRepository usuarioRepository;

    public MetaService(MetaRepository metaRepository, UsuarioRepository usuarioRepository) {
        this.metaRepository = metaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Crea la meta, la participación del creador y su primer registro en una sola transacción. */
    @Auditar(accion = AccionLog.CREAR_META, entidad = EntidadLog.META)
    @Transactional
    public MetaResponse crearMeta(CrearMetaRequest solicitud, Integer codigoUsuario) {
        validar(solicitud);

        Usuario creador = usuarioRepository.findById(codigoUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(codigoUsuario));

        Meta meta = new Meta(solicitud.nombre().trim(), solicitud.plazo(), solicitud.tipo());
        meta.crear(creador);
        return MetaResponse.desde(metaRepository.save(meta));
    }

    /**
     * HU3 + HU27: lista las metas del usuario filtradas por estado, tipo y plazo.
     * Sin estado devuelve las ACTIVAS (como la HU3); tipo y plazo nulos no filtran.
     * Devuelve lista vacía si ninguna meta coincide. Las CUMPLIDA salen por fecha de cumplimiento.
     */
    @Auditar(accion = AccionLog.LISTAR_METAS, entidad = EntidadLog.META)
    @Transactional(readOnly = true)
    public List<MetaResponse> listarMetas(Integer codigoUsuario, FiltroEstado estado,
                                          TipoMeta tipo, Plazo plazo) {
        if (!usuarioRepository.existsById(codigoUsuario)) {
            throw new UsuarioNoEncontradoException(codigoUsuario);
        }
        FiltroEstado filtroEstado = estado == null ? FiltroEstado.ACTIVA : estado;

        // Para el log: con qué filtros se consultó.
        ContextoAuditoria.agregar("filtroEstado", filtroEstado);
        if (tipo != null) ContextoAuditoria.agregar("filtroTipo", tipo);
        if (plazo != null) ContextoAuditoria.agregar("filtroPlazo", plazo);

        List<MetaResponse> metas = metaRepository.findByParticipante(codigoUsuario).stream()
                .filter(m -> filtroEstado.incluye(m.getEstado()))
                .filter(m -> tipo == null || m.getTipo() == tipo)
                .filter(m -> plazo == null || m.getPlazo() == plazo)
                .map(MetaResponse::desde)
                .toList();
        // Las cumplidas (antes en Mis logros) van de la cumplida más recientemente a la más antigua.
        // El orden es estable, así a igual fecha se mantiene el de creación.
        if (filtroEstado == FiltroEstado.CUMPLIDA) {
            return metas.stream()
                    .sorted(Comparator.comparing(MetaResponse::fechaCumplida,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }
        return metas;
    }

    /** HU21 (Mis logros): metas cumplidas del usuario, con la fecha en que se cumplieron. */
    @Auditar(accion = AccionLog.LISTAR_LOGROS, entidad = EntidadLog.META)
    @Transactional(readOnly = true)
    public List<MetaResponse> listarCumplidas(Integer codigoUsuario) {
        if (!usuarioRepository.existsById(codigoUsuario)) {
            throw new UsuarioNoEncontradoException(codigoUsuario);
        }
        return metaRepository.findCumplidasByParticipante(codigoUsuario).stream()
                .map(MetaResponse::desde)
                .toList();
    }

    /** HU2: edita nombre, plazo y tipo. Solo el creador edita y una meta CUMPLIDA no se edita. */
    @Auditar(accion = AccionLog.EDITAR_META, entidad = EntidadLog.META)
    @Transactional
    public MetaResponse editarMeta(Integer codigoMeta, CrearMetaRequest solicitud, Integer codigoUsuario) {
        validar(solicitud);

        Meta meta = metaRepository.findById(codigoMeta)
                .orElseThrow(() -> new MetaNoEncontradaException(codigoMeta));

        if (!meta.getCreador().getCodigoUsuario().equals(codigoUsuario)) {
            throw new PermisoDenegadoException("Solo el creador puede editar la meta");
        }
        if (meta.getEstado() == EstadoMeta.CUMPLIDA) {
            throw new ValidacionException(List.of("Una meta cumplida no se puede editar"));
        }

        // Para el log: valores anteriores (los nuevos salen de la respuesta)
        ContextoAuditoria.agregar("nombreAnterior", meta.getNombre());
        ContextoAuditoria.agregar("plazoAnterior", meta.getPlazo());
        ContextoAuditoria.agregar("tipoAnterior", meta.getTipo());

        meta.editar(solicitud.nombre().trim(), solicitud.plazo(), solicitud.tipo());
        return MetaResponse.desde(metaRepository.save(meta));
    }

    /** HU4: elimina una meta. Solo su creador puede eliminarla. */
    @Auditar(accion = AccionLog.ELIMINAR_META, entidad = EntidadLog.META)
    @Transactional
    public void eliminarMeta(Integer codigoMeta, Integer codigoUsuario) {
        Meta meta = metaRepository.findById(codigoMeta)
                .orElseThrow(() -> new MetaNoEncontradaException(codigoMeta));

        if (!meta.getCreador().getCodigoUsuario().equals(codigoUsuario)) {
            throw new PermisoDenegadoException(
                    "Solo el creador puede eliminar la meta"
            );
        }
        metaRepository.delete(meta);
    }

    /**
     * HU6: marca la meta como CUMPLIDA. Solo el creador puede hacerlo, la fecha la pone el
     * servidor y no se puede deshacer. Si ya estaba cumplida no cambia nada y conserva la
     * fecha original (doble clic, dos pestañas abiertas).
     */
    @Auditar(accion = AccionLog.MARCAR_CUMPLIDA, entidad = EntidadLog.META)
    @Transactional
    public MetaResponse marcarCumplida(Integer codigoMeta, Integer codigoUsuario) {
        Meta meta = metaRepository.findById(codigoMeta)
                .orElseThrow(() -> new MetaNoEncontradaException(codigoMeta));

        if (!meta.getCreador().getCodigoUsuario().equals(codigoUsuario)) {
            throw new PermisoDenegadoException("Solo el creador puede marcar la meta como cumplida");
        }
        if (meta.getEstado() == EstadoMeta.CUMPLIDA) {
            return MetaResponse.desde(meta);
        }

        meta.marcarCumplida(codigoUsuario, LocalDate.now());
        return MetaResponse.desde(metaRepository.save(meta));
    }

    private void validar(CrearMetaRequest s) {
        List<String> errores = new ArrayList<>();
        if (s == null || s.nombre() == null || s.nombre().isBlank()) {
            errores.add("El nombre es obligatorio");
        } else if (s.nombre().trim().length() > NOMBRE_MAX) {
            errores.add("El nombre no puede superar " + NOMBRE_MAX + " caracteres");
        }
        if (s == null || s.plazo() == null) errores.add("El plazo es obligatorio");
        if (s == null || s.tipo() == null) errores.add("El tipo es obligatorio");

        if (!errores.isEmpty()) throw new ValidacionException(errores);
    }
}