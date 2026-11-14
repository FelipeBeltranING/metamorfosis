import { useEffect, useMemo, useState } from 'react';
import { ChartColumn, ChevronLeft } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Encabezado from '../components/Encabezado';
import EstadoError from '../components/EstadoError';
import { obtenerEstadisticas } from '../services/subMetaService';
import {
  aFecha, aIso, diaYMes, nombresDias, nombresMeses,
} from '../utils/fechas';
import { detalleSubMeta, formatearNumero } from '../utils/subMetas';

const ALTO_BARRAS = 180;

/** Lunes de la semana de una fecha ISO. */
function lunesDe(iso) {
  const f = aFecha(iso);
  f.setDate(f.getDate() - ((f.getDay() + 6) % 7));
  return aIso(f);
}

/** Agrupa los días por semana o por mes: suma de valores, días cumplidos y días que contaron. */
function agrupar(dias, hoy, clave) {
  const grupos = new Map();
  dias.forEach((d) => {
    const k = clave(d.fecha);
    if (!grupos.has(k)) grupos.set(k, { clave: k, suma: 0, cumplidos: 0, cuentan: 0, tocan: 0 });
    const g = grupos.get(k);
    if (d.valor !== null) g.suma += Number(d.valor);
    if (d.toca) g.tocan += 1;
    if (d.toca && (d.fecha < hoy || d.valor !== null)) {
      g.cuentan += 1;
      if (d.cumplido) g.cumplidos += 1;
    }
  });
  return [...grupos.values()];
}

function Tarjeta({ titulo, valor, nota }) {
  return (
    <div className="rounded-2xl border border-zinc-200 bg-white px-4 py-3.5">
      <dt className="text-[13px] font-bold text-gray-600">{titulo}</dt>
      <dd className="mt-1 text-2xl font-extrabold">{valor}</dd>
      {nota && <dd className="text-[13px] text-gray-600">{nota}</dd>}
    </div>
  );
}

/** HU29: constancia, rachas e historial de un hábito para quien lo consulta. */
export default function EstadisticasSubMetaPage() {
  const { t } = useTranslation();
  const { codigoSubMeta } = useParams();
  const navigate = useNavigate();
  const [estado, setEstado] = useState({ fase: 'cargando' });
  const [intento, setIntento] = useState(0);
  const [vista, setVista] = useState(null);

  useEffect(() => {
    let activo = true;
    obtenerEstadisticas(codigoSubMeta)
      .then((datos) => activo && setEstado({ fase: 'lista', datos }))
      .catch((error) => {
        if (!activo) return;
        if (error.estado === 401) navigate('/login', { replace: true });
        else setEstado({ fase: 'error', mensaje: error.estado === 404 ? t('estadisticas.noExiste') : error.message });
      });
    return () => {
      activo = false;
    };
  }, [codigoSubMeta, intento, navigate, t]);

  const datos = estado.datos;
  const calculos = useMemo(() => {
    if (!datos) return null;
    const { dias, hoy, subMeta } = datos;
    const medible = subMeta.tipo === 'MEDIBLE';
    const objetivo = Number(subMeta.objetivo);
    const semanas = agrupar(dias, hoy, lunesDe).slice(-8);
    const meses = agrupar(dias, hoy, (f) => f.slice(0, 7)).slice(-6);
    const ultimos7 = dias.slice(-7);
    const delMes = dias.filter((d) => d.fecha.slice(0, 7) === hoy.slice(0, 7));
    const hoyDia = dias.at(-1);
    return {
      medible, objetivo, semanas, meses, hoyDia,
      suma7: ultimos7.reduce((s, d) => s + (d.valor !== null ? Number(d.valor) : 0), 0),
      tocan7: ultimos7.filter((d) => d.toca).length,
      sumaMes: delMes.reduce((s, d) => s + (d.valor !== null ? Number(d.valor) : 0), 0),
      tocanMes: (() => {
        const f = aFecha(hoy);
        const total = new Date(f.getFullYear(), f.getMonth() + 1, 0).getDate();
        let n = 0;
        for (let i = 1; i <= total; i += 1) {
          const dia = new Date(f.getFullYear(), f.getMonth(), i);
          if (subMeta.dias.includes(((dia.getDay() + 6) % 7) + 1)) n += 1;
        }
        return n;
      })(),
    };
  }, [datos]);

  if (estado.fase !== 'lista') {
    return (
      <div className="min-h-screen bg-slate-50 text-gray-900">
        <Encabezado />
        <main className="mx-auto w-full max-w-[1040px] px-4 pt-8">
          {estado.fase === 'cargando' && <div aria-busy="true" className="h-[480px] animate-pulse rounded-2xl bg-white" />}
          {estado.fase === 'error' && <EstadoError mensaje={estado.mensaje} onReintentar={() => { setEstado({ fase: 'cargando' }); setIntento((n) => n + 1); }} />}
        </main>
      </div>
    );
  }

  const { subMeta, hoy, dias } = datos;
  const {
    medible, objetivo, semanas, meses, hoyDia, suma7, tocan7, sumaMes, tocanMes,
  } = calculos;
  const vistaActual = vista ?? (medible ? 'dia' : 'semana');
  const sinDatos = datos.diasRegistrados === 0;
  const unidad = medible ? subMeta.unidad : '';

  // Barras del historial según la vista elegida.
  let barras;
  let linea = null;
  if (vistaActual === 'dia') {
    const ult = dias.slice(-14);
    const max = Math.max(objetivo * 1.3, ...ult.map((d) => Number(d.valor ?? 0)));
    linea = { alto: (objetivo / max) * ALTO_BARRAS, texto: t('estadisticas.objetivoLinea', { valor: formatearNumero(objetivo), unidad }) };
    barras = ult.map((d) => ({
      clave: d.fecha,
      texto: d.valor === null ? (d.toca ? '—' : '') : formatearNumero(d.valor),
      alto: ((Number(d.valor ?? 0)) / max) * ALTO_BARRAS,
      fuerte: d.cumplido,
      rotulo: d.fecha.slice(8) === '01' ? diaYMes(d.fecha) : String(Number(d.fecha.slice(8))),
      etiqueta: t('estadisticas.barraDia', { dia: diaYMes(d.fecha), valor: d.valor === null ? t('estadisticas.sinRegistro') : `${formatearNumero(d.valor)} ${unidad}` }),
    }));
  } else {
    const grupos = vistaActual === 'semana' ? semanas : meses;
    const rotulo = (g) => (vistaActual === 'semana' ? diaYMes(g.clave) : nombresMeses()[Number(g.clave.slice(5)) - 1]);
    if (medible) {
      const metaDe = (g) => objetivo * g.tocan;
      const max = Math.max(1, ...grupos.map((g) => Math.max(g.suma, metaDe(g))));
      barras = grupos.map((g) => ({
        clave: g.clave, texto: formatearNumero(g.suma), alto: (g.suma / max) * ALTO_BARRAS,
        fuerte: subMeta.tipoObjetivo === 'COMO_MAXIMO' ? g.suma <= metaDe(g) : g.suma >= metaDe(g),
        rotulo: rotulo(g), etiqueta: t('estadisticas.barraGrupo', { rotulo: rotulo(g), valor: formatearNumero(g.suma), unidad }),
      }));
    } else {
      const max = Math.max(1, ...grupos.map((g) => g.tocan));
      barras = grupos.map((g) => ({
        clave: g.clave, texto: `${g.cumplidos}/${g.tocan}`, alto: (g.cumplidos / max) * ALTO_BARRAS,
        fuerte: g.tocan > 0 && g.cumplidos / g.tocan >= 0.8,
        rotulo: rotulo(g), etiqueta: t('estadisticas.barraDias', { rotulo: rotulo(g), hechos: g.cumplidos, total: g.tocan }),
      }));
    }
  }

  // Calendario: 10 semanas (columnas) de lunes a domingo, terminando en la semana de hoy.
  const porFecha = Object.fromEntries(dias.map((d) => [d.fecha, d]));
  const inicioCal = aFecha(lunesDe(hoy));
  inicioCal.setDate(inicioCal.getDate() - 63);
  const columnas = Array.from({ length: 10 }, (_, w) => {
    const celdas = Array.from({ length: 7 }, (__, k) => {
      const f = new Date(inicioCal);
      f.setDate(f.getDate() + w * 7 + k);
      const iso = aIso(f);
      return { iso, dia: porFecha[iso] };
    });
    const conMes = celdas.find((c) => c.iso.slice(8) === '01');
    return { clave: celdas[0].iso, celdas, mes: w === 0 || conMes ? nombresMeses()[aFecha((conMes ?? celdas[0]).iso).getMonth()] : '' };
  });
  const colorCelda = (c) => {
    if (!c.dia) return 'bg-white text-transparent';
    if (!c.dia.toca) return 'bg-slate-100 text-zinc-400';
    if (c.dia.cumplido) return 'bg-violet-600 text-white';
    if (c.dia.valor !== null) return medible ? 'bg-violet-200 text-gray-800' : 'bg-orange-100 text-orange-700';
    return c.iso < hoy ? 'border border-zinc-200 bg-white text-gray-500' : 'border-2 border-gray-900 bg-white text-gray-700';
  };

  const pestanas = (medible ? ['dia', 'semana', 'mes'] : ['semana', 'mes']).map((id) => [id, t(`estadisticas.${id}`)]);
  const maxRacha = Math.max(1, ...datos.mejoresRachas.map((r) => r.dias));

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />
      <main className="mx-auto flex w-full max-w-[1040px] flex-col gap-5 px-4 pb-16 pt-6 sm:pt-8">
        <Link to={`/metas/${subMeta.codigoMeta}`} className="inline-flex items-center gap-1 self-start text-[15px] font-bold text-violet-700 hover:underline">
          <ChevronLeft className="size-[18px]" aria-hidden="true" /> {datos.nombreMeta}
        </Link>
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-extrabold sm:text-3xl">{subMeta.nombre}</h1>
            {subMeta.pregunta && <p className="mt-1 text-base text-gray-600 sm:text-lg">{subMeta.pregunta}</p>}
            <div className="mt-2.5 flex flex-wrap gap-1.5 text-[13px] font-bold">
              <span className={`rounded-lg px-2.5 py-1.5 ${medible ? 'bg-violet-100 text-violet-800' : 'bg-emerald-50 text-emerald-800'}`}>{medible ? t('estadisticas.medible') : t('estadisticas.siNo')}</span>
              <span className="rounded-lg bg-slate-100 px-2.5 py-1.5 font-semibold text-gray-600">{detalleSubMeta(subMeta)}</span>
            </div>
          </div>
          {!datos.soloLectura && (
            <div className="flex gap-2">
              <Link to={`/metas/${subMeta.codigoMeta}`} className="flex h-11 items-center rounded-xl bg-violet-600 px-4 font-bold text-white hover:bg-violet-700">{t('estadisticas.registrar')}</Link>
              <Link to={`/submetas/${subMeta.codigoSubMeta}/editar`} className="flex h-11 items-center rounded-xl border border-zinc-200 bg-white px-4 font-bold text-gray-700 hover:bg-gray-50">{t('estadisticas.editar')}</Link>
            </div>
          )}
        </div>

        {sinDatos ? (
          <>
            <section className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-violet-300 bg-white px-6 py-12 text-center">
              <div className="flex size-16 items-center justify-center rounded-2xl bg-violet-50 text-violet-600"><ChartColumn className="size-8" aria-hidden="true" /></div>
              <h2 className="text-xl font-extrabold">{t('estadisticas.sinDatosTitulo')}</h2>
              <p className="max-w-lg text-[15px] leading-6 text-gray-600">
                {t('estadisticas.sinDatosTexto')}
              </p>
              {!datos.soloLectura && (
                <Link to={`/metas/${subMeta.codigoMeta}`} className="mt-2 flex h-12 items-center rounded-xl bg-violet-600 px-6 font-bold text-white hover:bg-violet-700">{t('estadisticas.registrarHoy')}</Link>
              )}
            </section>
            <dl className="grid grid-cols-2 gap-3 sm:grid-cols-4">
              <Tarjeta titulo={t('estadisticas.ultimos7')} valor="—" />
              <Tarjeta titulo={t('estadisticas.ultimos30')} valor="—" />
              <Tarjeta titulo={t('estadisticas.rachaActual')} valor="—" />
              <Tarjeta titulo={t('estadisticas.mejorRacha')} valor="—" />
            </dl>
          </>
        ) : (
          <>
            <dl className="grid grid-cols-2 gap-3 sm:grid-cols-[repeat(auto-fit,minmax(150px,1fr))]">
              <Tarjeta titulo={t('estadisticas.ultimos7')} valor={datos.constancia7 === null ? '—' : `${datos.constancia7}%`} nota={t('estadisticas.deCumplimiento')} />
              <Tarjeta titulo={t('estadisticas.ultimos30')} valor={datos.constancia30 === null ? '—' : `${datos.constancia30}%`} nota={t('estadisticas.deCumplimiento')} />
              <Tarjeta titulo={t('estadisticas.rachaActual')} valor={t('estadisticas.dias', { count: datos.rachaActual })} nota={subMeta.dias.length < 7 ? t('estadisticas.noCuentan') : t('estadisticas.seguidos')} />
              <Tarjeta
                titulo={t('estadisticas.mejorRacha')}
                valor={t('estadisticas.dias', { count: datos.mejorRacha })}
                nota={datos.mejoresRachas[0] ? `${diaYMes(datos.mejoresRachas[0].inicio)} – ${diaYMes(datos.mejoresRachas[0].fin)}` : ''}
              />
              {medible
                ? <Tarjeta titulo={t('estadisticas.total')} valor={`${formatearNumero(datos.total)} ${unidad}`} nota={t('estadisticas.promedio', { valor: formatearNumero(datos.promedio) })} />
                : <Tarjeta titulo={t('estadisticas.diasHechos')} valor={datos.diasCumplidos} nota={t('estadisticas.deRegistrados', { total: datos.diasRegistrados })} />}
            </dl>

            {medible && (
              <section aria-labelledby="t-objetivo" className="rounded-2xl border border-zinc-200 bg-white p-5 sm:p-6">
                <h2 id="t-objetivo" className="mb-3.5 text-lg font-extrabold">{t('estadisticas.objetivoPeriodo')}</h2>
                <div className="flex flex-col gap-2.5">
                  {[
                    [t('estadisticas.hoy'), hoyDia?.valor !== null && hoyDia?.valor !== undefined ? Number(hoyDia.valor) : 0, hoyDia?.toca ? objetivo : 0],
                    [t('estadisticas.ultimos7'), suma7, objetivo * tocan7],
                    [nombresMeses()[aFecha(hoy).getMonth()].replace(/^./, (l) => l.toUpperCase()), sumaMes, objetivo * tocanMes],
                  ].map(([nombre, valor, meta]) => {
                    const cumple = meta > 0 && (subMeta.tipoObjetivo === 'COMO_MAXIMO' ? valor <= meta : valor >= meta);
                    const ancho = meta > 0 ? Math.min(100, Math.round((100 * valor) / meta)) : 0;
                    return (
                      <div key={nombre} className="grid grid-cols-[96px_minmax(0,1fr)] items-center gap-3 sm:grid-cols-[120px_minmax(0,1fr)_150px]">
                        <span className="text-sm font-bold text-gray-700">{nombre}</span>
                        <div role="img" aria-label={t('estadisticas.progreso', { nombre, valor: formatearNumero(valor), meta: formatearNumero(meta), unidad })} className="h-3.5 overflow-hidden rounded-full bg-slate-100">
                          <div className={`h-3.5 rounded-full ${cumple ? 'bg-emerald-500' : 'bg-violet-400'}`} style={{ width: `${ancho}%` }} />
                        </div>
                        <span className="col-span-2 text-right text-sm font-bold text-gray-700 sm:col-span-1">
                          {meta > 0 ? `${formatearNumero(valor)} / ${formatearNumero(meta)} ${unidad}` : t('estadisticas.noToca')}
                        </span>
                      </div>
                    );
                  })}
                </div>
                {subMeta.tipoObjetivo === 'COMO_MAXIMO' && (
                  <p className="mt-3 text-[13px] text-gray-600">{t('estadisticas.notaMaximo')}</p>
                )}
              </section>
            )}

            {/* HU29: el historial detallado va plegado; arriba quedan la constancia y las rachas. */}
            <details className="rounded-2xl border border-zinc-200 bg-white p-5 sm:p-6">
              <summary className="cursor-pointer">
                <span className="text-lg font-extrabold">{medible ? t('estadisticas.historial') : t('estadisticas.diasCumplidos')}</span>
                <span className="ml-2 text-sm font-semibold text-violet-700">{t('hitos.verHistorial')}</span>
              </summary>
              <div className="mb-4 mt-4 flex flex-wrap items-center justify-end gap-3">
                <div role="group" aria-label={t('estadisticas.agruparPor')} className="flex gap-1 rounded-xl bg-slate-100 p-1">
                  {pestanas.map(([id, nombre]) => (
                    <button key={id} type="button" aria-pressed={vistaActual === id} onClick={() => setVista(id)} className={`h-9 rounded-[9px] px-3.5 text-sm font-bold ${vistaActual === id ? 'bg-white text-violet-800 shadow-sm' : 'text-gray-600'}`}>{nombre}</button>
                  ))}
                </div>
              </div>
              <div className="overflow-x-auto">
                <div className="min-w-[520px]">
                  <div className="relative flex items-end gap-2 border-b border-zinc-200" style={{ height: ALTO_BARRAS + 28 }}>
                    {linea && (
                      <>
                        <div aria-hidden="true" className="absolute inset-x-0 border-t-2 border-dashed border-violet-400" style={{ bottom: linea.alto }} />
                        <span className="absolute right-0 -translate-y-1 bg-white pl-1 text-xs font-bold text-violet-800" style={{ bottom: linea.alto }}>{linea.texto}</span>
                      </>
                    )}
                    {barras.map((b) => (
                      <div key={b.clave} className="flex h-full flex-1 flex-col items-center justify-end gap-1">
                        <span className="text-xs font-extrabold text-gray-700">{b.texto}</span>
                        <div role="img" aria-label={b.etiqueta} className={`w-full max-w-10 rounded-t-md ${b.fuerte ? 'bg-violet-600' : 'bg-violet-200'}`} style={{ height: Math.max(2, b.alto) }} />
                      </div>
                    ))}
                  </div>
                  <div className="mt-1.5 flex gap-2">
                    {barras.map((b) => <span key={b.clave} className="flex-1 text-center text-xs font-semibold text-gray-600">{b.rotulo}</span>)}
                  </div>
                </div>
              </div>
            </details>

            <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
              <section aria-labelledby="t-calendario" className="rounded-2xl border border-zinc-200 bg-white p-5 sm:p-6">
                <h2 id="t-calendario" className="mb-3.5 text-lg font-extrabold">{t('estadisticas.calendario')}</h2>
                <div className="flex gap-1.5 overflow-x-auto">
                  <div className="flex flex-col gap-1 pt-5">
                    {nombresDias().map((n) => <span key={n} className="flex h-7 items-center text-[11px] font-bold text-gray-600">{n.slice(0, 1)}</span>)}
                  </div>
                  {columnas.map((col) => (
                    <div key={col.clave} className="flex flex-col gap-1">
                      <span className="h-4 text-[11px] font-bold text-gray-600">{col.mes}</span>
                      {col.celdas.map((c) => (
                        <span
                          key={c.iso}
                          title={c.dia ? `${diaYMes(c.iso)}: ${!c.dia.toca ? t('estadisticas.celdaNoToca') : c.dia.valor === null ? t('estadisticas.sinRegistro') : medible ? `${formatearNumero(c.dia.valor)} ${unidad}` : c.dia.cumplido ? t('estadisticas.hecho') : t('estadisticas.noHecho')}` : ''}
                          className={`flex size-7 items-center justify-center rounded-md text-[11px] font-bold ${colorCelda(c)}`}
                        >
                          {c.dia ? Number(c.iso.slice(8)) : ''}
                        </span>
                      ))}
                    </div>
                  ))}
                </div>
                <div className="mt-3.5 flex flex-wrap gap-x-3.5 gap-y-1.5 text-xs font-semibold text-gray-600">
                  <span className="flex items-center gap-1.5"><span aria-hidden="true" className="size-3.5 rounded bg-violet-600" />{t('estadisticas.leyendaCumplido')}</span>
                  <span className="flex items-center gap-1.5"><span aria-hidden="true" className={`size-3.5 rounded ${medible ? 'bg-violet-200' : 'bg-orange-100'}`} />{medible ? t('estadisticas.leyendaSinLlegar') : t('estadisticas.leyendaNoHecho')}</span>
                  <span className="flex items-center gap-1.5"><span aria-hidden="true" className="size-3.5 rounded border border-zinc-200" />{t('estadisticas.leyendaSinRegistro')}</span>
                  <span className="flex items-center gap-1.5"><span aria-hidden="true" className="size-3.5 rounded bg-slate-100" />{t('estadisticas.leyendaNoToca')}</span>
                </div>
              </section>

              <section aria-labelledby="t-rachas" className="rounded-2xl border border-zinc-200 bg-white p-5 sm:p-6">
                <h2 id="t-rachas" className="mb-3.5 text-lg font-extrabold">{t('estadisticas.mejoresRachas')}</h2>
                {datos.mejoresRachas.length === 0 ? (
                  <p className="text-[15px] text-gray-600">{t('estadisticas.sinRachas')}</p>
                ) : (
                  <ol className="flex flex-col gap-2">
                    {datos.mejoresRachas.map((r, i) => (
                      <li key={`${r.inicio}-${r.fin}`}>
                        <span
                          className={`flex h-8 items-center justify-center rounded-lg text-sm font-extrabold ${i === 0 ? 'bg-violet-600 text-white' : 'bg-violet-100 text-violet-800'}`}
                          style={{ width: `${Math.max(40, Math.round((100 * r.dias) / maxRacha))}%` }}
                        >
                          {t('estadisticas.dias', { count: r.dias })} · {diaYMes(r.inicio)}{r.inicio !== r.fin && ` – ${diaYMes(r.fin)}`}
                        </span>
                      </li>
                    ))}
                  </ol>
                )}
                <h3 className="mb-2.5 mt-6 text-base font-extrabold">{t('estadisticas.queDias')}</h3>
                <div className="flex h-28 items-end gap-2">
                  {datos.porDiaSemana.map((p, i) => (p === null ? null : (
                    <div key={nombresDias()[i]} className="flex h-full flex-1 flex-col items-center justify-end gap-1">
                      <span className="text-xs font-extrabold text-gray-700">{p}%</span>
                      <div role="img" aria-label={`${nombresDias()[i]}: ${p}%`} className="w-full max-w-8 rounded-t-md bg-violet-400" style={{ height: Math.max(2, p * 0.7) }} />
                      <span className="text-xs font-bold text-gray-600">{nombresDias()[i]}</span>
                    </div>
                  )))}
                </div>
              </section>
            </div>
          </>
        )}
      </main>
    </div>
  );
}
