import { useEffect, useRef, useState } from 'react';
import { ChartColumn, EllipsisVertical, Pencil, Trash2 } from 'lucide-react';
import { Link } from 'react-router-dom';

/** Sub-metas: menú ⋮ de una sub-meta en "Tu camino" (estadísticas, editar, eliminar). */
export default function MenuSubMeta({ subMeta, puedeEditar, onEliminar }) {
  const [abierto, setAbierto] = useState(false);
  const contenedor = useRef(null);
  const boton = useRef(null);

  useEffect(() => {
    if (!abierto) return undefined;
    const alTocarFuera = (e) => {
      if (!contenedor.current?.contains(e.target)) setAbierto(false);
    };
    const alPresionarTecla = (e) => {
      if (e.key === 'Escape') {
        setAbierto(false);
        boton.current?.focus();
      }
    };
    document.addEventListener('pointerdown', alTocarFuera);
    document.addEventListener('keydown', alPresionarTecla);
    return () => {
      document.removeEventListener('pointerdown', alTocarFuera);
      document.removeEventListener('keydown', alPresionarTecla);
    };
  }, [abierto]);

  const clase = 'flex h-11 items-center gap-2.5 rounded-lg px-3 text-[15px] font-bold hover:bg-slate-50';
  return (
    <div ref={contenedor} className="relative">
      <button
        ref={boton}
        type="button"
        onClick={() => setAbierto((a) => !a)}
        aria-label={`Opciones de ${subMeta.nombre}`}
        aria-haspopup="menu"
        aria-expanded={abierto}
        className={`flex size-11 items-center justify-center rounded-lg ${abierto ? 'bg-violet-100 text-violet-800' : 'text-gray-500 hover:bg-gray-100'}`}
      >
        <EllipsisVertical className="size-5" aria-hidden="true" />
      </button>
      {abierto && (
        <div role="menu" className="absolute left-0 top-12 z-20 flex w-56 flex-col rounded-xl border border-zinc-200 bg-white p-1.5 shadow-xl">
          <Link role="menuitem" to={`/submetas/${subMeta.codigoSubMeta}`} className={`${clase} text-gray-700`}>
            <ChartColumn className="size-[18px]" aria-hidden="true" />
            Ver estadísticas
          </Link>
          {puedeEditar && (
            <>
              <Link role="menuitem" to={`/submetas/${subMeta.codigoSubMeta}/editar`} className={`${clase} text-gray-700`}>
                <Pencil className="size-[18px]" aria-hidden="true" />
                Editar
              </Link>
              <button
                type="button"
                role="menuitem"
                onClick={() => {
                  setAbierto(false);
                  onEliminar(subMeta);
                }}
                className={`${clase} text-left text-red-700`}
              >
                <Trash2 className="size-[18px]" aria-hidden="true" />
                Eliminar
              </button>
            </>
          )}
        </div>
      )}
    </div>
  );
}
