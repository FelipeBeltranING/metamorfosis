import { Clock } from 'lucide-react';
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

export default function MetaTarjeta({ meta }) {
  const tipo = TIPO_POR_VALOR[meta.tipo] ?? TIPO_POR_VALOR.OTRO;
  const plazo = PLAZO_POR_VALOR[meta.plazo];

  return (
    <li className="flex flex-col gap-5 rounded-2xl border border-zinc-200 bg-white p-5 shadow-[0_4px_20px_rgba(30,27,46,0.03)] transition hover:border-violet-300 sm:p-8">
      <div className="flex items-center justify-between">
        <div className="flex size-12 items-center justify-center rounded-xl bg-violet-50">
          <tipo.Icono className="size-6 text-violet-600" aria-hidden="true" />
        </div>
        <span className="flex items-center gap-1.5 rounded-xl bg-emerald-400 px-3 py-2 text-sm font-bold text-gray-900">
          <span className="text-[10px]" aria-hidden="true">●</span>
          Activa
        </span>
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