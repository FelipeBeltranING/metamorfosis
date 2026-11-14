import { Check, Flag } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import MenuSubMeta from './MenuSubMeta';
import { formatearFecha } from '../utils/fechas';

/**
 * HU5 y HU29: los hitos de una meta en "Tu camino". Cada hito se marca una sola vez (cualquier
 * participante, porque en una meta compartida el hito es del equipo) y la barra muestra el avance
 * real de la meta: hitos logrados sobre el total.
 */
export default function HitosCamino({
  camino, ocupado, onAlternar, onEliminar,
}) {
  const { t } = useTranslation();
  const { hitos, avance } = camino;
  const bloqueado = camino.soloLectura;

  return (
    <section aria-labelledby="titulo-hitos" className="flex flex-col gap-3 rounded-2xl border border-zinc-200 bg-white p-4 sm:p-7">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 id="titulo-hitos" className="flex items-center gap-2 text-xl font-extrabold sm:text-2xl">
            <Flag className="size-5 text-violet-600" aria-hidden="true" />
            {t('hitos.titulo')}
          </h2>
          <p className="text-sm text-gray-600 sm:text-[15px]">{t('hitos.explicacion')}</p>
        </div>
        {camino.puedeEditar && (
          <Link to={`/metas/${camino.codigoMeta}/submetas/nueva?tipo=HITO`} className="flex h-11 items-center rounded-xl border border-violet-300 bg-violet-50 px-4 text-[15px] font-bold text-violet-800 hover:bg-violet-100">
            {t('hitos.agregarHito')}
          </Link>
        )}
      </div>

      {hitos.length === 0 ? (
        <p className="rounded-xl bg-slate-50 p-4 text-[15px] leading-6 text-gray-600">
          {camino.puedeEditar ? t('hitos.vacioCreador') : t('hitos.vacioInvitado')}
        </p>
      ) : (
        <>
          <div className="flex items-center gap-3">
            <div
              role="progressbar"
              aria-valuemin={0}
              aria-valuemax={100}
              aria-valuenow={avance.porcentaje ?? 0}
              aria-label={t('hitos.avanceEtiqueta', { porcentaje: avance.porcentaje ?? 0, logrados: avance.logrados, total: avance.total })}
              className="h-3 flex-1 overflow-hidden rounded-full bg-violet-100"
            >
              <div className="h-full rounded-full bg-violet-600 transition-[width]" style={{ width: `${avance.porcentaje ?? 0}%` }} />
            </div>
            <span className="text-sm font-extrabold text-gray-700">{t('hitos.avance', { logrados: avance.logrados, total: avance.total })}</span>
          </div>

          <ul className="flex flex-col gap-2">
            {hitos.map((hito) => {
              const logrado = Boolean(hito.fechaLogrado);
              const guardando = ocupado === `hito-${hito.codigoSubMeta}`;
              return (
                <li key={hito.codigoSubMeta} className={`flex items-center gap-3 rounded-xl px-2 py-2 ${logrado ? 'bg-emerald-50' : 'bg-slate-50'}`}>
                  <button
                    type="button"
                    disabled={bloqueado || guardando}
                    onClick={() => onAlternar(hito)}
                    aria-pressed={logrado}
                    aria-label={logrado ? t('hitos.desmarcar', { nombre: hito.nombre }) : t('hitos.marcar', { nombre: hito.nombre })}
                    className={`flex size-10 shrink-0 items-center justify-center rounded-lg border-2 ${
                      logrado ? 'border-emerald-500 bg-emerald-500 text-white' : 'border-zinc-300 bg-white'
                    } ${bloqueado ? 'cursor-default' : 'hover:ring-2 hover:ring-violet-200'} disabled:opacity-100`}
                  >
                    {logrado && <Check className="size-5" strokeWidth={3} aria-hidden="true" />}
                  </button>
                  <div className="min-w-0 flex-1">
                    <span className={`block truncate text-[15px] font-bold sm:text-base ${logrado ? 'text-gray-600 line-through decoration-emerald-600/60' : 'text-gray-900'}`}>
                      {hito.nombre}
                    </span>
                    <span className="block truncate text-[12px] font-semibold text-gray-600 sm:text-[13px]">
                      {!logrado && t('hitos.pendiente')}
                      {logrado && (camino.participantes?.length > 1 && hito.logradoPor
                        ? t('hitos.logradoPor', { fecha: formatearFecha(hito.fechaLogrado), nombre: hito.logradoPor })
                        : t('hitos.logradoEl', { fecha: formatearFecha(hito.fechaLogrado) }))}
                    </span>
                  </div>
                  <MenuSubMeta subMeta={hito} puedeEditar={camino.puedeEditar} onEliminar={onEliminar} />
                </li>
              );
            })}
          </ul>
        </>
      )}
    </section>
  );
}
