package com.metamorfosis.backend.service;

import com.metamorfosis.backend.dto.CrearMetaRequest;
import com.metamorfosis.backend.dto.MetaResponse;
import com.metamorfosis.backend.exception.UsuarioNoEncontradoException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.Usuario;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    @Transactional
    public MetaResponse crearMeta(CrearMetaRequest solicitud, Integer codigoUsuario) {
        validar(solicitud);

        Usuario creador = usuarioRepository.findById(codigoUsuario)
                .orElseThrow(() -> new UsuarioNoEncontradoException(codigoUsuario));

        Meta meta = new Meta(solicitud.nombre().trim(), solicitud.plazo(), solicitud.tipo());
        meta.crear(creador);
        return MetaResponse.desde(metaRepository.save(meta));
    }

    /** Lista las metas activas del usuario (HU3). Devuelve lista vacía si no tiene ninguna. */
    @Transactional(readOnly = true)
    public List<MetaResponse> listarMetasActivas(Integer codigoUsuario) {
        if (!usuarioRepository.existsById(codigoUsuario)) {
            throw new UsuarioNoEncontradoException(codigoUsuario);
        }
        return metaRepository.findActivasByParticipante(codigoUsuario).stream()
                .map(MetaResponse::desde)
                .toList();
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