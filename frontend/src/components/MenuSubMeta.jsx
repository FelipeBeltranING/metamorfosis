import { useEffect, useRef, useState } from 'react';
import { ChartColumn, EllipsisVertical, Pencil, Trash2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

/**
 * HU28: menú ⋮ de un paso en "Tu camino" (editar, eliminar). Los hábitos además llevan a sus
 * estadísticas (HU29); un hito no tiene estadísticas por día, así que sin permiso de edición no hay menú.
 */
export default function MenuSubMeta({ subMeta, puedeEditar, onEliminar }) {
  const { t } = useTranslation();
  const [abierto, setAbierto] = useState(false);
  const contenedor = useRef(null);
  const boton = useRef(null);
  const esHito = subMeta.tipo === 'HITO';

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
  if (esHito && !puedeEditar) return null;
  return (
    <div ref={contenedor} className="relative">
      <button
        ref={boton}
        type="button"
        onClick={() => setAbierto((a) => !a)}
        aria-label={t('menuSubMeta.opciones', { nombre: subMeta.nombre })}
        aria-haspopup="menu"
        aria-expanded={abierto}
        className={`flex size-11 items-center justify-center rounded-lg ${abierto ? 'bg-violet-100 text-violet-800' : 'text-gray-500 hover:bg-gray-100'}`}
      >
        <EllipsisVertical className="size-5" aria-hidden="true" />
      </button>
      {abierto && (
        <div role="menu" className={`absolute top-12 z-20 flex w-56 flex-col rounded-xl border border-zinc-200 bg-white p-1.5 shadow-xl ${esHito ? 'right-0' : 'left-0'}`}>
          {!esHito && (
            <Link role="menuitem" to={`/submetas/${subMeta.codigoSubMeta}`} className={`${clase} text-gray-700`}>
              <ChartColumn className="size-[18px]" aria-hidden="true" />
              {t('menuSubMeta.estadisticas')}
            </Link>
          )}
          {puedeEditar && (
            <>
              <Link role="menuitem" to={`/submetas/${subMeta.codigoSubMeta}/editar`} className={`${clase} text-gray-700`}>
                <Pencil className="size-[18px]" aria-hidden="true" />
                {t('menuSubMeta.editar')}
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
                {t('menuSubMeta.eliminar')}
              </button>
            </>
          )}
        </div>
      )}
    </div>
  );
}
