import {
  Check, Clock, EyeOff, Share2,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import i18n from '../i18n';
import { formatearFecha } from '../utils/fechas';
import MenuTarjeta from './MenuTarjeta';
import AnilloConstancia from './AnilloConstancia';
import { AvataresEncimados } from './Participantes';
import { textoParticipantes } from '../utils/participantes';
import { obtenerSesion } from '../services/sesionService';
import { formatearNumero } from '../utils/subMetas';
import { PLAZOS, TIPOS } from '../constants/opcionesMeta';

const TIPO_POR_VALOR = Object.fromEntries(TIPOS.map((x) => [x.valor, x]));
const PLAZO_POR_VALOR = Object.fromEntries(PLAZOS.map((p) => [p.valor, p]));

function Chip({ Icono, texto }) {
  return (
    <span className="flex items-center gap-1.5 rounded-xl bg-slate-100 px-3 py-2 text-sm font-semibold text-gray-600">
      <Icono className="size-4" aria-hidden="true" />
      {texto}
    </span>
  );
}

/**
 * HU6: círculo de la tarjeta. Con la meta activa es un botón que abre la confirmación;
 * con la meta cumplida solo muestra el check, porque no se puede desmarcar.
 */
function CirculoCumplida({ nombreMeta, cumplida, onClick }) {
  const { t } = useTranslation();
  const base = 'flex size-10 shrink-0 items-center justify-center rounded-full border';
  if (cumplida) {
    return (
      <span
        role="img"
        aria-label={t('tarjeta.estaCumplida', { nombre: nombreMeta })}
        title={t('tarjeta.metaCumplida')}
        className={`${base} border-emerald-500 bg-emerald-500 text-white`}
      >
        <Check className="size-5" aria-hidden="true" />
      </span>
    );
  }
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={t('tarjeta.marcarNombre', { nombre: nombreMeta })}
      title={t('tarjeta.marcar')}
      className={`${base} border-zinc-300 bg-white text-transparent transition hover:border-emerald-400 hover:text-emerald-300 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300`}
    >
      <Check className="size-5" aria-hidden="true" />
    </button>
  );
}

/** HU5: valor de hoy de un hábito en la tarjeta ("20 / 15 págs", "Hecho", "Pendiente"). */
function valorHoy({ subMeta, dia }) {
  if (dia.valor === null || dia.valor === undefined) return i18n.t('tarjeta.pendiente');
  if (subMeta.tipo === 'SI_NO') return Number(dia.valor) >= 1 ? i18n.t('tarjeta.hecho') : i18n.t('tarjeta.noHecho');
  return `${formatearNumero(dia.valor)} / ${formatearNumero(subMeta.objetivo)} ${subMeta.unidad}`;
}

/** HU25: la meta no se muestra en el perfil público del usuario. El texto lo lee el lector de pantalla. */
export function EtiquetaOculta({ className = '' }) {
  const { t } = useTranslation();
  return (
    <span
      title={t('tarjeta.noSeMuestra')}
      className={`flex items-center gap-1.5 rounded-xl bg-slate-100 px-3 py-2 text-sm font-bold text-gray-700 ${className}`}
    >
      <EyeOff className="size-4" aria-hidden="true" />
      {t('tarjeta.oculta')}
    </span>
  );
}

export default function MetaTarjeta({
  meta, resumen, onMarcarCumplida, onEditar, onCambiarVisibilidad, onEliminar, onCompartir, onInvitar,
}) {
  const { t } = useTranslation();
  const tipo = TIPO_POR_VALOR[meta.tipo] ?? TIPO_POR_VALOR.OTRO;
  const plazo = PLAZO_POR_VALOR[meta.plazo];
  const cumplida = meta.estado === 'CUMPLIDA';
  // HU13: es compartida si alguien más participa o está invitado. Solo quien la creó la edita,
  // la elimina, la marca cumplida e invita; los demás participantes registran sus días.
  const participantes = meta.participantes ?? [];
  const compartida = participantes.length > 1;
  const soyCreador = participantes.length === 0 || participantes.some((p) => p.creador && p.codigoUsuario === obtenerSesion()?.codigoUsuario);

  return (
    <li className={`flex flex-col gap-5 rounded-2xl border p-5 shadow-[0_4px_20px_rgba(30,27,46,0.03)] transition sm:p-8 ${
      cumplida ? 'border-emerald-300 bg-emerald-50/40' : 'border-zinc-200 bg-white hover:border-violet-300'
    }`}
    >
      <div className="flex items-center justify-between">
        <div className="flex size-12 items-center justify-center rounded-xl bg-violet-50">
          <tipo.Icono className="size-6 text-violet-600" aria-hidden="true" />
        </div>
        <div className="flex flex-wrap items-center justify-end gap-2">
          {meta.oculta && <EtiquetaOculta />}
          {cumplida ? (
            <span className="rounded-xl bg-violet-600 px-3 py-2 text-sm font-bold text-white">{t('tarjeta.cumplida')}</span>
          ) : (
            <span className="flex items-center gap-1.5 rounded-xl bg-emerald-100 px-3 py-2 text-sm font-bold text-emerald-800">
              <span className="text-[10px]" aria-hidden="true">●</span>
              {t('tarjeta.activa')}
            </span>
          )}
          {onMarcarCumplida && (soyCreador || cumplida) && (
            <CirculoCumplida
              nombreMeta={meta.nombre}
              cumplida={cumplida}
              onClick={() => onMarcarCumplida(meta)}
            />
          )}
          {(onEditar || onCambiarVisibilidad || onEliminar || onInvitar) && (
            <MenuTarjeta
              nombre={meta.nombre}
              onEditar={onEditar && soyCreador ? () => onEditar(meta) : undefined}
              editarBloqueado={cumplida}
              oculta={meta.oculta}
              onCambiarVisibilidad={onCambiarVisibilidad && (() => onCambiarVisibilidad(meta))}
              onEliminar={onEliminar && soyCreador ? () => onEliminar(meta) : undefined}
              onInvitar={onInvitar && soyCreador ? () => onInvitar(meta) : undefined}
            />
          )}
        </div>
      </div>
      <h2 className="line-clamp-2 break-words text-xl font-bold text-gray-900 sm:text-2xl">
        {meta.nombre}
      </h2>
      <div className="flex items-start justify-between gap-3">
        <div className="flex flex-wrap gap-2">
          <Chip Icono={tipo.Icono} texto={tipo.etiqueta} />
          {plazo && <Chip Icono={Clock} texto={plazo.etiqueta} />}
        </div>
        {/* HU29: el anillo es el avance real de la meta (hitos logrados), no la constancia. */}
        {resumen?.avance?.total > 0 && (
          <AnilloConstancia
            porcentaje={resumen.avance.porcentaje}
            etiqueta={t('hitos.avanceEtiqueta', { porcentaje: resumen.avance.porcentaje, logrados: resumen.avance.logrados, total: resumen.avance.total })}
          />
        )}
      </div>
      {compartida && (
        <button
          type="button"
          onClick={onInvitar && soyCreador && !cumplida ? () => onInvitar(meta) : undefined}
          disabled={!(onInvitar && soyCreador && !cumplida)}
          className="-mt-1 flex items-center gap-3 self-start rounded-xl text-left text-sm font-semibold text-gray-600 enabled:hover:text-violet-700 disabled:cursor-default"
        >
          <AvataresEncimados personas={participantes} />
          <span>{t('tarjeta.compartida', { detalle: textoParticipantes(participantes, obtenerSesion()?.codigoUsuario) })}</span>
        </button>
      )}

      {/* HU5 y HU29: los hábitos que tocan hoy, la constancia y el acceso a "Tu camino". */}
      {resumen && !cumplida && (
        resumen.totalSubMetas > 0 ? (
          <div className="flex flex-col gap-2.5 border-t border-zinc-100 pt-4">
            {resumen.avance?.total > 0 && (
              <p className="text-sm font-bold text-violet-800">
                {resumen.avance.logrados === resumen.avance.total
                  ? t('hitos.todosLogrados')
                  : `${t('hitos.avance', { logrados: resumen.avance.logrados, total: resumen.avance.total })} · ${t('hitos.faltan', { count: resumen.avance.total - resumen.avance.logrados })}`}
              </p>
            )}
            {(resumen.tocanHoy > 0 || resumen.avance?.total !== resumen.totalSubMetas) && (
              <p className="text-sm font-bold text-gray-600">
                {resumen.tocanHoy > 0
                  ? t('hitos.tarjetaHoy', { hechas: resumen.cumplidasHoy, total: resumen.tocanHoy })
                  : t('hitos.tarjetaHoyNada')}
              </p>
            )}
            <ul className="flex flex-col gap-2">
              {resumen.hoy.map((item) => (
                <li key={item.subMeta.codigoSubMeta} className="flex items-center gap-2.5 text-[15px]">
                  {item.dia.cumplido ? (
                    <span aria-hidden="true" className="flex size-[22px] shrink-0 items-center justify-center rounded-full bg-emerald-500 text-white">
                      <Check className="size-3.5" strokeWidth={3} />
                    </span>
                  ) : (
                    <span aria-hidden="true" className="block size-[22px] shrink-0 rounded-full border-2 border-zinc-300" />
                  )}
                  <span className="min-w-0 flex-1 truncate">{item.subMeta.nombre}</span>
                  <span className={`text-sm font-bold ${item.dia.cumplido ? 'text-emerald-700' : 'text-gray-600'}`}>
                    {valorHoy(item)}
                  </span>
                </li>
              ))}
            </ul>
            {resumen.constancia7 !== null && resumen.constancia7 !== undefined && (
              <p className="text-[13px] font-semibold text-gray-600">{t('hitos.constanciaSemana', { porcentaje: resumen.constancia7 })}</p>
            )}
            <Link
              to={`/metas/${meta.codigoMeta}`}
              className="mt-1 flex h-11 items-center justify-center rounded-xl border border-violet-300 bg-violet-50 font-bold text-violet-800 hover:bg-violet-100"
            >
              {t('tarjeta.verCamino')}
            </Link>
          </div>
        ) : (
          <div className="flex flex-col gap-3 border-t border-zinc-100 pt-4">
            <p className="text-[15px] leading-6 text-gray-600">
              {soyCreador
                ? t('hitos.tarjetaSinCaminoCreador')
                : t('hitos.tarjetaSinCaminoInvitado')}
            </p>
            {soyCreador && (
              <Link
                to={`/metas/${meta.codigoMeta}`}
                className="flex h-11 items-center justify-center rounded-xl bg-violet-600 font-bold text-white hover:bg-violet-700"
              >
                {t('hitos.tarjetaPrimerPaso')}
              </Link>
            )}
          </div>
        )
      )}
      <div className="flex flex-wrap items-center justify-between gap-2 border-t border-zinc-100 pt-4 text-sm text-gray-500">
        {meta.fechaCreacion && <span>{t('tarjeta.creadaEl', { fecha: formatearFecha(meta.fechaCreacion) })}</span>}
        {cumplida && meta.fechaCumplida ? (
          <span className="flex items-center gap-1 font-semibold text-emerald-700">
            <Check className="size-4" aria-hidden="true" />
            {t('tarjeta.cumplidaEl', { fecha: formatearFecha(meta.fechaCumplida) })}
          </span>
        ) : (
          <span>{t('hitos.enCurso')}</span>
        )}
      </div>
      {/* HU6 + sub-metas: el camino de una meta cumplida queda para consultar. */}
      {cumplida && onCompartir && (
        <Link
          to={`/metas/${meta.codigoMeta}`}
          className="flex h-11 items-center justify-center rounded-xl border border-zinc-200 bg-white font-semibold text-gray-700 hover:bg-gray-50"
        >
          {t('tarjeta.verRecorrido')}
        </Link>
      )}
      {/* HU21: una meta cumplida se puede compartir como logro. */}
      {cumplida && onCompartir && (
        <button
          type="button"
          onClick={() => onCompartir(meta)}
          className="flex h-11 items-center justify-center gap-2 rounded-xl border border-violet-200 bg-white font-semibold text-violet-700 hover:bg-violet-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
        >
          <Share2 className="size-4" aria-hidden="true" />
          {t('tarjeta.compartirLogro')}
        </button>
      )}
    </li>
  );
}
