import { useId } from 'react';
import { X } from 'lucide-react';
import { PLAZOS, TIPOS } from '../constants/opcionesMeta';
import { ESTADOS_FILTRO, hayFiltros } from '../utils/filtrosMetas';

const CLASE_SELECT = 'h-11 w-full rounded-xl border border-zinc-200 bg-white px-3 text-[15px] font-semibold text-gray-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300 sm:w-48';

/**
 * HU27: barra de filtros de Mis metas. Estado con botones (Activas / Cumplidas / Todas),
 * tipo y plazo con listas desplegables. Avisa cuántas metas se muestran.
 */
export default function FiltrosMetas({
  filtros, cantidad, cargando, onCambiar, onLimpiar,
}) {
  const id = useId();
  const cambiar = (campo, valor) => onCambiar({ ...filtros, [campo]: valor });

  return (
    <section aria-label="Filtrar metas" className="mb-6 flex flex-col gap-3">
      <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-end">
        <fieldset className="flex flex-col gap-1.5">
          <legend className="mb-1.5 text-sm font-bold text-gray-600">Estado</legend>
          <div className="flex rounded-xl border border-zinc-200 bg-white p-1">
            {ESTADOS_FILTRO.map((opcion) => {
              const elegido = filtros.estado === opcion.valor;
              return (
                <button
                  key={opcion.valor}
                  type="button"
                  aria-pressed={elegido}
                  onClick={() => cambiar('estado', opcion.valor)}
                  className={`h-9 flex-1 rounded-lg px-4 text-[15px] font-bold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300 sm:flex-none ${
                    elegido ? 'bg-violet-600 text-white' : 'text-gray-600 hover:bg-slate-100'
                  }`}
                >
                  {opcion.etiqueta}
                </button>
              );
            })}
          </div>
        </fieldset>

        <div className="flex flex-col gap-1.5">
          <label htmlFor={`${id}-tipo`} className="text-sm font-bold text-gray-600">Tipo</label>
          <select
            id={`${id}-tipo`}
            value={filtros.tipo}
            onChange={(e) => cambiar('tipo', e.target.value)}
            className={CLASE_SELECT}
          >
            <option value="">Todos los tipos</option>
            {TIPOS.map((t) => <option key={t.valor} value={t.valor}>{t.etiqueta}</option>)}
          </select>
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor={`${id}-plazo`} className="text-sm font-bold text-gray-600">Plazo</label>
          <select
            id={`${id}-plazo`}
            value={filtros.plazo}
            onChange={(e) => cambiar('plazo', e.target.value)}
            className={CLASE_SELECT}
          >
            <option value="">Todos los plazos</option>
            {PLAZOS.map((p) => <option key={p.valor} value={p.valor}>{p.etiqueta}</option>)}
          </select>
        </div>

        {hayFiltros(filtros) && (
          <button
            type="button"
            onClick={onLimpiar}
            className="flex h-11 items-center justify-center gap-1.5 rounded-xl px-3 text-[15px] font-bold text-violet-700 hover:bg-violet-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
          >
            <X className="size-4" aria-hidden="true" />
            Limpiar filtros
          </button>
        )}
      </div>

      <p aria-live="polite" className="text-sm text-gray-500">
        {cargando ? 'Buscando metas...' : `${cantidad} ${cantidad === 1 ? 'meta' : 'metas'}`}
      </p>
    </section>
  );
}
