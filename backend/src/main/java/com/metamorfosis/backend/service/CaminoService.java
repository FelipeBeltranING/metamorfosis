package com.metamorfosis.backend.service;

import com.metamorfosis.backend.config.CalendarioUsuario;
import com.metamorfosis.backend.dto.AvanceResponse;
import com.metamorfosis.backend.dto.CaminoResponse;
import com.metamorfosis.backend.dto.ParticipanteResponse;
import com.metamorfosis.backend.dto.DiaResponse;
import com.metamorfosis.backend.dto.EstadisticasResponse;
import com.metamorfosis.backend.dto.EstadisticasResponse.Racha;
import com.metamorfosis.backend.dto.ResumenCaminoResponse;
import com.metamorfosis.backend.dto.SubMetaCaminoResponse;
import com.metamorfosis.backend.dto.SubMetaResponse;
import com.metamorfosis.backend.exception.MetaNoEncontradaException;
import com.metamorfosis.backend.exception.PermisoDenegadoException;
import com.metamorfosis.backend.exception.SubMetaNoEncontradaException;
import com.metamorfosis.backend.exception.ValidacionException;
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
 * Lo que se muestra del camino de una meta: "Tu camino" (hitos y hábitos), el resumen para las
 * tarjetas de "Mis metas" (HU3) y las estadísticas de un hábito (HU29).
 * El avance de la meta sale de los hitos; la constancia, de los hábitos. Las reglas de los hábitos
 * están en {@link CalculoCamino}.
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

    /** "Tu camino" de una meta: hitos con su avance (HU29) y hábitos con sus últimos 7 días y constancias (HU5). */
    @Transactional(readOnly = true)
    public CaminoResponse camino(Integer codigoMeta, Integer codigoUsuario) {
        Meta meta = metaRepository.findById(codigoMeta)
                .orElseThrow(() -> new MetaNoEncontradaException(codigoMeta));
        exigirParticipante(codigoMeta, codigoUsuario);

        LocalDate hoy = calendario.hoy();
        List<SubMeta> pasos = subMetaRepository.findByMetaCodigoMetaOrderByCodigoSubMetaAsc(codigoMeta);
        List<SubMeta> hitos = pasos.stream().filter(SubMeta::esHito).toList();
        List<SubMeta> subMetas = pasos.stream().filter(SubMeta::esHabito).toList();
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
                avance(hitos),
                CalculoCamino.constancia(todos, hoy, 7), CalculoCamino.constancia(todos, hoy, 30),
                cumplidasHoy, tocanHoy,
                CalculoCamino.diasSeguidosCompletos(new ArrayList<>(dias.values()), hoy),
                registroRepository.countBySubMetaMetaCodigoMeta(codigoMeta),
                hitos.stream().map(SubMetaResponse::desde).toList(),
                items,
                meta.participantesVisibles().stream().map(p -> ParticipanteResponse.desde(p, meta.getCreador())).toList());
    }

    /** Para cada meta activa del usuario (HU3): avance por hitos (HU29), constancia de 7 días y hábitos de hoy (HU5). */
    @Transactional(readOnly = true)
    public List<ResumenCaminoResponse> resumenes(Integer codigoUsuario) {
        LocalDate hoy = calendario.hoy();
        List<ResumenCaminoResponse> resumenes = new ArrayList<>();
        for (Meta meta : metaRepository.findActivasByParticipante(codigoUsuario)) {
            List<SubMeta> pasos = subMetaRepository.findByMetaCodigoMetaOrderByCodigoSubMetaAsc(meta.getCodigoMeta());
            List<SubMeta> subMetas = pasos.stream().filter(SubMeta::esHabito).toList();
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
            resumenes.add(new ResumenCaminoResponse(meta.getCodigoMeta(), pasos.size(),
                    avance(pasos.stream().filter(SubMeta::esHito).toList()),
                    registroRepository.countBySubMetaMetaCodigoMeta(meta.getCodigoMeta()),
                    CalculoCamino.constancia(todos, hoy, 7), cumplidas, deHoy.size(), deHoy));
        }
        return resumenes;
    }

    /** HU29: estadísticas de un hábito para el usuario: constancias, rachas, totales e historial diario. */
    @Transactional(readOnly = true)
    public EstadisticasResponse estadisticas(Integer codigoSubMeta, Integer codigoUsuario) {
        SubMeta subMeta = subMetaRepository.findById(codigoSubMeta)
                .orElseThrow(() -> new SubMetaNoEncontradaException(codigoSubMeta));
        Meta meta = subMeta.getMeta();
        exigirParticipante(meta.getCodigoMeta(), codigoUsuario);
        if (subMeta.esHito()) {
            throw new ValidacionException(List.of("Un hito no tiene estadísticas por día"));
        }

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

    /** HU29: hitos logrados sobre el total. */
    private static AvanceResponse avance(List<SubMeta> hitos) {
        int logrados = (int) hitos.stream().filter(SubMeta::isLogrado).count();
        return AvanceResponse.de(logrados, hitos.size());
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
