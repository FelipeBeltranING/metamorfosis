package com.metamorfosis.backend.service;

import com.metamorfosis.backend.config.CalendarioUsuario;
import com.metamorfosis.backend.dto.CaminoResponse;
import com.metamorfosis.backend.dto.DiaResponse;
import com.metamorfosis.backend.dto.EstadisticasResponse;
import com.metamorfosis.backend.dto.EstadisticasResponse.Racha;
import com.metamorfosis.backend.dto.ResumenCaminoResponse;
import com.metamorfosis.backend.dto.SubMetaCaminoResponse;
import com.metamorfosis.backend.dto.SubMetaResponse;
import com.metamorfosis.backend.exception.MetaNoEncontradaException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.SubMetaNoEncontradaException;
import com.metamorfosis.backend.model.EstadoMeta;
import com.metamorfosis.backend.model.Meta;
import com.metamorfosis.backend.model.RegistroSubMeta;
import com.metamorfosis.backend.model.SubMeta;
import com.metamorfosis.backend.model.TipoSubMeta;
import com.metamorfosis.backend.repository.MetaRepository;
import com.metamorfosis.backend.repository.RegistroSubMetaRepository;
import com.metamorfosis.backend.repository.SubMetaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sub-metas: lo que se muestra del camino. "Tu camino" de una meta, el resumen de hoy para las
 * tarjetas de "Mis metas" y las estadísticas de una sub-meta. Las reglas están en {@link CalculoCamino}.
 */
@Service
public class CaminoService {

    static final int DIAS_CAMINO = 30;
    static final int DIAS_ESTADISTICAS = 365;

    private final MetaRepository metaRepository;
    private final SubMetaRepository subMetaRepository;
    private final RegistroSubMetaRepository registroRepository;
    private final CalendarioUsuario calendario;

    public CaminoService(MetaRepository metaRepository, SubMetaRepository subMetaRepository,
                         RegistroSubMetaRepository registroRepository, CalendarioUsuario calendario) {
        this.metaRepository = metaRepository;
        this.subMetaRepository = subMetaRepository;
        this.registroRepository = registroRepository;
        this.calendario = calendario;
    }

    /** "Tu camino" de una meta: sub-metas con los últimos 7 días y las constancias del usuario. */
    @Transactional(readOnly = true)
    public CaminoResponse camino(Integer codigoMeta, Integer codigoUsuario) {
        Meta meta = metaRepository.findById(codigoMeta)
                .orElseThrow(() -> new MetaNoEncontradaException(codigoMeta));
        exigirParticipante(codigoMeta, codigoUsuario);

        LocalDate hoy = calendario.hoy();
        List<SubMeta> subMetas = subMetaRepository.findByMetaCodigoMetaOrderByCodigoSubMetaAsc(codigoMeta);
        Map<Integer, List<DiaResponse>> dias = diasPorSubMeta(subMetas, codigoUsuario, hoy.minusDays(DIAS_CAMINO - 1L), hoy);

        List<DiaResponse> todos = new ArrayList<>();
        List<SubMetaCaminoResponse> items = new ArrayList<>();
        int tocanHoy = 0;
        int cumplidasHoy = 0;
        for (SubMeta s : subMetas) {
            List<DiaResponse> d = dias.get(s.getCodigoSubMeta());
            todos.addAll(d);
            DiaResponse deHoy = d.get(d.size() - 1);
            if (deHoy.toca()) {
                tocanHoy++;
                if (deHoy.cumplido()) cumplidasHoy++;
            }
            List<DiaResponse> ultimos7 = new ArrayList<>(d.subList(d.size() - 7, d.size()));
            Collections.reverse(ultimos7);
            items.add(new SubMetaCaminoResponse(SubMetaResponse.desde(s), ultimos7,
                    CalculoCamino.constancia(d, hoy, 7)));
        }

        boolean soloLectura = meta.getEstado() != EstadoMeta.ACTIVA;
        boolean esCreador = meta.getCreador().getCodigoUsuario().equals(codigoUsuario);
        return new CaminoResponse(
                meta.getCodigoMeta(), meta.getNombre(), meta.getTipo(), meta.getPlazo(), meta.getEstado(),
                meta.getFechaCreacion(), meta.getFechaCumplida(meta.getCreador().getCodigoUsuario()), hoy,
                soloLectura, esCreador && !soloLectura,
                CalculoCamino.constancia(todos, hoy, 7), CalculoCamino.constancia(todos, hoy, 30),
                cumplidasHoy, tocanHoy,
                CalculoCamino.diasSeguidosCompletos(new ArrayList<>(dias.values()), hoy),
                registroRepository.countBySubMetaMetaCodigoMeta(codigoMeta),
                items);
    }

    /** Para cada meta activa del usuario: constancia de 7 días y sub-metas que tocan hoy (HU3). */
    @Transactional(readOnly = true)
    public List<ResumenCaminoResponse> resumenes(Integer codigoUsuario) {
        LocalDate hoy = calendario.hoy();
        List<ResumenCaminoResponse> resumenes = new ArrayList<>();
        for (Meta meta : metaRepository.findActivasByParticipante(codigoUsuario)) {
            List<SubMeta> subMetas = subMetaRepository.findByMetaCodigoMetaOrderByCodigoSubMetaAsc(meta.getCodigoMeta());
            Map<Integer, List<DiaResponse>> dias = diasPorSubMeta(subMetas, codigoUsuario, hoy.minusDays(6), hoy);

            List<DiaResponse> todos = new ArrayList<>();
            List<ResumenCaminoResponse.HoyItem> deHoy = new ArrayList<>();
            int cumplidas = 0;
            for (SubMeta s : subMetas) {
                List<DiaResponse> d = dias.get(s.getCodigoSubMeta());
                todos.addAll(d);
                DiaResponse hoyDia = d.get(d.size() - 1);
                if (hoyDia.toca()) {
                    deHoy.add(new ResumenCaminoResponse.HoyItem(SubMetaResponse.desde(s), hoyDia));
                    if (hoyDia.cumplido()) cumplidas++;
                }
            }
            resumenes.add(new ResumenCaminoResponse(meta.getCodigoMeta(), subMetas.size(),
                    registroRepository.countBySubMetaMetaCodigoMeta(meta.getCodigoMeta()),
                    CalculoCamino.constancia(todos, hoy, 7), cumplidas, deHoy.size(), deHoy));
        }
        return resumenes;
    }

    /** Estadísticas de una sub-meta para el usuario: constancias, rachas, totales e historial diario. */
    @Transactional(readOnly = true)
    public EstadisticasResponse estadisticas(Integer codigoSubMeta, Integer codigoUsuario) {
        SubMeta subMeta = subMetaRepository.findById(codigoSubMeta)
                .orElseThrow(() -> new SubMetaNoEncontradaException(codigoSubMeta));
        Meta meta = subMeta.getMeta();
        exigirParticipante(meta.getCodigoMeta(), codigoUsuario);

        LocalDate hoy = calendario.hoy();
        LocalDate desde = subMeta.getFechaCreacion();
        if (desde.isBefore(hoy.minusDays(DIAS_ESTADISTICAS - 1L))) desde = hoy.minusDays(DIAS_ESTADISTICAS - 1L);
        if (desde.isAfter(hoy)) desde = hoy;
        List<DiaResponse> dias = diasPorSubMeta(List.of(subMeta), codigoUsuario, desde, hoy).get(codigoSubMeta);

        List<Racha> rachas = CalculoCamino.rachas(dias, hoy);
        int cumplidos = 0;
        int registrados = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (DiaResponse d : dias) {
            if (d.valor() == null) continue;
            registrados++;
            if (d.cumplido()) cumplidos++;
            total = total.add(d.valor());
        }
        boolean medible = subMeta.getTipo() == TipoSubMeta.MEDIBLE;
        BigDecimal promedio = medible && registrados > 0
                ? total.divide(BigDecimal.valueOf(registrados), 2, RoundingMode.HALF_UP) : null;

        return new EstadisticasResponse(
                SubMetaResponse.desde(subMeta), meta.getNombre(), meta.getEstado() != EstadoMeta.ACTIVA, hoy,
                CalculoCamino.constancia(dias, hoy, 7), CalculoCamino.constancia(dias, hoy, 30),
                CalculoCamino.rachaActual(dias, hoy), rachas.isEmpty() ? 0 : rachas.get(0).dias(),
                rachas.subList(0, Math.min(5, rachas.size())),
                cumplidos, registrados, medible ? total : null, promedio,
                CalculoCamino.porDiaSemana(dias, hoy), dias);
    }

    private void exigirParticipante(Integer codigoMeta, Integer codigoUsuario) {
        if (!metaRepository.esParticipanteAceptado(codigoMeta, codigoUsuario)) {
            throw new PermisoDenegadoException("No participas en esta meta");
        }
    }

    /** Días (del más antiguo al más reciente) de cada sub-meta, con lo que registró el usuario. */
    private Map<Integer, List<DiaResponse>> diasPorSubMeta(List<SubMeta> subMetas, Integer codigoUsuario,
                                                           LocalDate desde, LocalDate hasta) {
        Map<Integer, Map<LocalDate, BigDecimal>> valores = new HashMap<>();
        if (!subMetas.isEmpty()) {
            List<Integer> codigos = subMetas.stream().map(SubMeta::getCodigoSubMeta).toList();
            for (RegistroSubMeta r : registroRepository
                    .findBySubMetaCodigoSubMetaInAndUsuarioCodigoUsuarioAndFechaBetween(codigos, codigoUsuario, desde, hasta)) {
                valores.computeIfAbsent(r.getSubMeta().getCodigoSubMeta(), k -> new HashMap<>())
                        .put(r.getFecha(), r.getValor());
            }
        }
        Map<Integer, List<DiaResponse>> resultado = new java.util.LinkedHashMap<>();
        for (SubMeta s : subMetas) {
            resultado.put(s.getCodigoSubMeta(), CalculoCamino.dias(s,
                    valores.getOrDefault(s.getCodigoSubMeta(), Map.of()), desde, hasta));
        }
        return resultado;
    }
}
