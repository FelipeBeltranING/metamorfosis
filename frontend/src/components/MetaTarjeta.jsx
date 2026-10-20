import { Check, Clock, LoaderCircle } from 'lucide-react';
import MenuTarjeta from './MenuTarjeta';
import { PLAZOS, TIPOS } from '../constants/opcionesMeta';

const TIPO_POR_VALOR = Object.fromEntries(TIPOS.map((t) => [t.valor, t]));
const PLAZO_POR_VALOR = Object.fromEntries(PLAZOS.map((p) => [p.valor, p]));

function Chip({ Icono, texto }) {
  return (
    <span className="flex items-center gap-1.5 rounded-xl bg-slate-100 px-3 py-2 text-sm font-semibold text-gray-600">
      <Icono className="size-4" aria-hidden="true" />
      {texto}
    </span>
  );
}

/** HU5: botón circular para marcar/desmarcar el cumplimiento de hoy con un solo toque. */
function BotonCumplimientoHoy({ nombreMeta, cumplidaHoy, marcando, onClick }) {
  const base = 'flex size-10 shrink-0 items-center justify-center rounded-full border transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300 disabled:cursor-not-allowed';
  const estado = cumplidaHoy
    ? 'border-emerald-400 bg-emerald-400 text-gray-900 hover:bg-emerald-500'
    : 'border-zinc-300 bg-white text-transparent hover:border-violet-400';

  return (
    <button
      type="button"
      onClick={onClick}
      disabled={marcando}
      aria-pressed={cumplidaHoy}
      aria-label={cumplidaHoy ? `Quitar cumplimiento de hoy de ${nombreMeta}` : `Marcar ${nombreMeta} como cumplida hoy`}
      title={cumplidaHoy ? 'Cumplida hoy' : 'Marcar como cumplida hoy'}
      className={`${base} ${estado}`}
    >
      {marcando
        ? <LoaderCircle className="size-4 animate-spin text-gray-400" aria-hidden="true" />
        : <Check className="size-5" aria-hidden="true" />}
    </button>
  );
}

export default function MetaTarjeta({
  meta, cumplidaHoy = false, marcandoHoy = false, onMarcarHoy, onRegistrarCumplimiento, onEditar, onEliminar,
}) {
  const tipo = TIPO_POR_VALOR[meta.tipo] ?? TIPO_POR_VALOR.OTRO;
  const plazo = PLAZO_POR_VALOR[meta.plazo];

  return (
    <li className="flex flex-col gap-5 rounded-2xl border border-zinc-200 bg-white p-5 shadow-[0_4px_20px_rgba(30,27,46,0.03)] transition hover:border-violet-300 sm:p-8">
      <div className="flex items-center justify-between">
        <div className="flex size-12 items-center justify-center rounded-xl bg-violet-50">
          <tipo.Icono className="size-6 text-violet-600" aria-hidden="true" />
        </div>
        <div className="flex items-center gap-2">
          <span className="flex items-center gap-1.5 rounded-xl bg-emerald-400 px-3 py-2 text-sm font-bold text-gray-900">
            <span className="text-[10px]" aria-hidden="true">●</span>
            Activa
          </span>
          {onMarcarHoy && (
            <BotonCumplimientoHoy
              nombreMeta={meta.nombre}
              cumplidaHoy={cumplidaHoy}
              marcando={marcandoHoy}
              onClick={() => onMarcarHoy(meta)}
            />
          )}
          {(onRegistrarCumplimiento || onEditar || onEliminar) && (
            <MenuTarjeta
              nombre={meta.nombre}
              onRegistrarCumplimiento={onRegistrarCumplimiento && (() => onRegistrarCumplimiento(meta))}
              onEditar={onEditar && (() => onEditar(meta))}
              onEliminar={onEliminar && (() => onEliminar(meta))}
            />
          )}
        </div>
      </div>
      <h2 className="line-clamp-2 break-words text-xl font-bold text-gray-900 sm:text-2xl">
        {meta.nombre}
      </h2>
      <div className="flex flex-wrap gap-2">
        <Chip Icono={tipo.Icono} texto={tipo.etiqueta} />
        {plazo && <Chip Icono={Clock} texto={plazo.etiqueta} />}
      </div>
    </li>
  );
}