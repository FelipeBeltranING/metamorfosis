package com.metamorfosis.backend.service;

import com.metamorfosis.backend.auditoria.Auditar;
import com.metamorfosis.backend.config.CalendarioUsuario;
import com.metamorfosis.backend.dto.DiaResponse;
import com.metamorfosis.backend.dto.RegistrarDiaRequest;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.SubMetaNoEncontradaException;
import com.metamorfosis.backend.exception.ValidacionException;
import com.metamorfosis.backend.model.AccionLog;
import com.metamorfosis.backend.model.EntidadLog;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.RegistroSubMeta;
import com.metamorfosis.backend.model.SubMeta;
import com.metamorfosis.backend.model.TipoSubMeta;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.RegistroSubMetaRepository;
import com.metamorfosis.backend.repository.SubMetaRepository;
import com.metamorfosis.backend.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * HU5: registrar (o corregir) y quitar lo que un participante hizo en un día en un hábito del camino.
 * Un registro por sub-meta, usuario y día. No se registran días futuros, días antes de que la
 * sub-meta existiera ni días que no tocan. Una meta cumplida queda en solo lectura.
 */
@Service
public class RegistroDiaService {

    private final SubMetaRepository subMetaRepository;
    private final RegistroSubMetaRepository registroRepository;
    private final MetaRepository metaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CalendarioUsuario calendario;

    public RegistroDiaService(SubMetaRepository subMetaRepository, RegistroSubMetaRepository registroRepository,
                              MetaRepository metaRepository, UsuarioRepository usuarioRepository,
                              CalendarioUsuario calendario) {
        this.subMetaRepository = subMetaRepository;
        this.registroRepository = registroRepository;
        this.metaRepository = metaRepository;
        this.usuarioRepository = usuarioRepository;
        this.calendario = calendario;
    }

    @Auditar(accion = AccionLog.REGISTRAR_DIA, entidad = EntidadLog.SUB_META)
    @Transactional
    public DiaResponse registrar(Integer codigoSubMeta, LocalDate fecha, RegistrarDiaRequest pedido,
                                 Integer codigoUsuario) {
        SubMeta subMeta = buscarParaRegistrar(codigoSubMeta, codigoUsuario);
        validarFecha(subMeta, fecha);
        BigDecimal valor = validarValor(subMeta, pedido == null ? null : pedido.valor());

        RegistroSubMeta registro = registroRepository
                .findBySubMetaCodigoSubMetaAndUsuarioCodigoUsuarioAndFecha(codigoSubMeta, codigoUsuario, fecha)
                .orElseGet(() -> new RegistroSubMeta(subMeta, usuarioRepository.getReferenceById(codigoUsuario),
                        fecha, valor));
        registro.cambiarValor(valor);
        registroRepository.save(registro);
        return new DiaResponse(fecha, true, valor, subMeta.cumple(valor));
    }

    /** Quita el registro de ese día (si no había, no pasa nada). */
    @Auditar(accion = AccionLog.QUITAR_REGISTRO, entidad = EntidadLog.SUB_META)
    @Transactional
    public void quitar(Integer codigoSubMeta, LocalDate fecha, Integer codigoUsuario) {
        buscarParaRegistrar(codigoSubMeta, codigoUsuario);
        if (fecha == null) throw new ValidacionException(List.of("La fecha es obligatoria"));
        registroRepository
                .findBySubMetaCodigoSubMetaAndUsuarioCodigoUsuarioAndFecha(codigoSubMeta, codigoUsuario, fecha)
                .ifPresent(registroRepository::delete);
    }

    private SubMeta buscarParaRegistrar(Integer codigoSubMeta, Integer codigoUsuario) {
        SubMeta subMeta = subMetaRepository.findById(codigoSubMeta)
                .orElseThrow(() -> new SubMetaNoEncontradaException(codigoSubMeta));
        Integer codigoMeta = subMeta.getMeta().getCodigoMeta();
        if (!metaRepository.esParticipanteAceptado(codigoMeta, codigoUsuario)) {
            throw new PermisoDenegadoException("Solo los participantes de la meta pueden registrar su día");
        }
        if (subMeta.getMeta().getEstado() != EstadoMeta.ACTIVA) {
            throw new ValidacionException(List.of(SubMetaService.MENSAJE_SOLO_LECTURA));
        }
        if (subMeta.esHito()) {
            throw new ValidacionException(List.of("Un hito no se registra por día: se marca como logrado"));
        }
        return subMeta;
    }

    private void validarFecha(SubMeta subMeta, LocalDate fecha) {
        if (fecha == null) throw new ValidacionException(List.of("La fecha es obligatoria"));
        if (fecha.isAfter(calendario.hoy())) {
            throw new ValidacionException(List.of("No puedes registrar días que aún no llegan"));
        }
        if (fecha.isBefore(subMeta.getFechaCreacion())) {
            throw new ValidacionException(List.of("Ese día la sub-meta todavía no existía"));
        }
        if (!subMeta.tocaEl(fecha)) {
            throw new ValidacionException(List.of("Esta sub-meta no toca ese día"));
        }
    }

    private BigDecimal validarValor(SubMeta subMeta, BigDecimal valor) {
        if (valor == null) throw new ValidacionException(List.of("El valor es obligatorio"));
        if (valor.signum() < 0) throw new ValidacionException(List.of("El valor no puede ser negativo"));
        if (subMeta.getTipo() == TipoSubMeta.SI_NO) {
            if (valor.compareTo(BigDecimal.ZERO) != 0 && valor.compareTo(BigDecimal.ONE) != 0) {
                throw new ValidacionException(List.of("En una sub-meta Sí / No el valor es 1 (hecho) o 0 (no hecho)"));
            }
            return valor.signum() == 0 ? BigDecimal.ZERO : BigDecimal.ONE;
        }
        if (valor.compareTo(SubMetaService.OBJETIVO_MAXIMO) > 0) {
            throw new ValidacionException(List.of("El valor no puede ser mayor que 1.000.000"));
        }
        if (valor.stripTrailingZeros().scale() > 2) {
            throw new ValidacionException(List.of("El valor admite máximo 2 decimales"));
        }
        return valor;
    }
}
