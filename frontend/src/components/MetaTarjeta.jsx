import { Check, Clock, LoaderCircle } from 'lucide-react';
import MenuTarjeta from './MenuTarjeta';
import { PLAZOS, TIPOS } from '../constants/opcionesMeta';

const TIPO_POR_VALOR = Object.fromEntries(TIPOS.map((t) => [t.valor, t]));
const PLAZO_POR_VALOR = Object.fromEntries(PLAZOS.map((p) => [p.valor, p]));

const FORMATO_FECHA = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric' });

/** "2026-11-01" → "1 nov 2026". Se arma con la fecha local para no correr el día por la zona horaria. */
function formatearFecha(iso) {
  if (!iso) return '';
  const [anio, mes, dia] = iso.split('-').map(Number);
  return FORMATO_FECHA.format(new Date(anio, mes - 1, dia)).replace('.', '');
}

function Chip({ Icono, texto }) {
  return (
    <span className="flex items-center gap-1.5 rounded-xl bg-slate-100 px-3 py-2 text-sm font-semibold text-gray-600">
      <Icono className="size-4" aria-hidden="true" />
      {texto}
    </span>
  );
}

/** HU6: círculo que marca la meta como cumplida o la devuelve a activa. */
function BotonCumplida({ nombreMeta, cumplida, guardando, onClick }) {
  const base = 'flex size-10 shrink-0 items-center justify-center rounded-full border transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300 disabled:cursor-not-allowed';
  const estado = cumplida
    ? 'border-emerald-500 bg-emerald-500 text-white hover:bg-emerald-600'
    : 'border-zinc-300 bg-white text-transparent hover:border-violet-400';

  return (
    <button
      type="button"
      onClick={onClick}
      disabled={guardando}
      aria-pressed={cumplida}
      aria-label={cumplida ? `Desmarcar ${nombreMeta} como cumplida` : `Marcar ${nombreMeta} como cumplida`}
      title={cumplida ? 'Desmarcar como cumplida' : 'Marcar como cumplida'}
      className={`${base} ${estado}`}
    >
      {guardando
        ? <LoaderCircle className="size-4 animate-spin text-gray-400" aria-hidden="true" />
        : <Check className="size-5" aria-hidden="true" />}
    </button>
  );
}

export default function MetaTarjeta({
  meta, guardando = false, onCambiarEstado, onEditar, onEliminar,
}) {
  const tipo = TIPO_POR_VALOR[meta.tipo] ?? TIPO_POR_VALOR.OTRO;
  const plazo = PLAZO_POR_VALOR[meta.plazo];
  const cumplida = meta.estado === 'CUMPLIDA';

  return (
    <li className={`flex flex-col gap-5 rounded-2xl border p-5 shadow-[0_4px_20px_rgba(30,27,46,0.03)] transition sm:p-8 ${
      cumplida ? 'border-emerald-300 bg-emerald-50/40' : 'border-zinc-200 bg-white hover:border-violet-300'
    }`}
    >
      <div className="flex items-center justify-between">
        <div className="flex size-12 items-center justify-center rounded-xl bg-violet-50">
          <tipo.Icono className="size-6 text-violet-600" aria-hidden="true" />
        </div>
        <div className="flex items-center gap-2">
          {cumplida ? (
            <span className="rounded-xl bg-violet-600 px-3 py-2 text-sm font-bold text-white">Cumplida</span>
          ) : (
            <span className="flex items-center gap-1.5 rounded-xl bg-emerald-100 px-3 py-2 text-sm font-bold text-emerald-800">
              <span className="text-[10px]" aria-hidden="true">●</span>
              Activa
            </span>
          )}
          {onCambiarEstado && (
            <BotonCumplida
              nombreMeta={meta.nombre}
              cumplida={cumplida}
              guardando={guardando}
              onClick={() => onCambiarEstado(meta, !cumplida)}
            />
          )}
          {(onEditar || onEliminar) && (
            <MenuTarjeta
              nombre={meta.nombre}
              onEditar={onEditar && (() => onEditar(meta))}
              editarBloqueado={cumplida}
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
      <div className="flex flex-wrap items-center justify-between gap-2 border-t border-zinc-100 pt-4 text-sm text-gray-500">
        {meta.fechaCreacion && <span>Creada el {formatearFecha(meta.fechaCreacion)}</span>}
        {cumplida && meta.fechaCumplida ? (
          <span className="flex items-center gap-1 font-semibold text-emerald-700">
            <Check className="size-4" aria-hidden="true" />
            Cumplida el {formatearFecha(meta.fechaCumplida)}
          </span>
        ) : (
          <span>Pendiente</span>
        )}
      </div>
    </li>
  );
}
