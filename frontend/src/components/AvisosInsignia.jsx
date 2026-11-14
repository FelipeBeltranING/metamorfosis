import { useCallback, useEffect, useRef, useState } from 'react';
import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { AvisosInsigniaContexto } from '../hooks/useAvisosInsignia';
import IconoInsignia from './IconoInsignia';
import { DURACION_AVISO_INSIGNIA_MS, descripcionInsignia, nombreInsignia } from '../utils/insignias';

/** Un aviso "¡Ganaste una insignia!". Se cierra solo a los 6 segundos o con la X. */
function AvisoInsignia({ id, insignia, onCerrar }) {
  const { t } = useTranslation();
  useEffect(() => {
    const temporizador = setTimeout(() => onCerrar(id), DURACION_AVISO_INSIGNIA_MS);
    return () => clearTimeout(temporizador);
  }, [id, onCerrar]);

  return (
    <div className="pointer-events-auto flex items-start gap-3 rounded-2xl border border-violet-200 bg-white p-4 shadow-[0_8px_24px_rgba(30,27,46,0.14)]">
      <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-violet-600">
        <IconoInsignia nombre={insignia.icono} className="size-7 text-white" aria-hidden="true" />
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-bold text-violet-700">{t('insignias.ganaste')}</p>
        <p className="text-base font-bold text-gray-900">{nombreInsignia(insignia)}</p>
        <p className="text-sm text-gray-600">{descripcionInsignia(insignia)}</p>
      </div>
      <button
        type="button"
        onClick={() => onCerrar(id)}
        aria-label={t('insignias.cerrarAviso', { nombre: nombreInsignia(insignia) })}
        className="flex size-8 shrink-0 items-center justify-center rounded-lg text-gray-500 hover:bg-slate-100 hover:text-gray-900 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
      >
        <X className="size-5" aria-hidden="true" />
      </button>
    </div>
  );
}

/**
 * HU11: muestra los avisos de las insignias ganadas. Envuelve las rutas para que el aviso
 * aparezca en cualquier pantalla donde se marque una meta (Mis metas y Tu camino).
 */
export default function AvisosInsigniaProvider({ children }) {
  const [avisos, setAvisos] = useState([]);
  const siguienteId = useRef(0);

  const mostrarInsignias = useCallback((insignias) => {
    if (!insignias?.length) return;
    const nuevos = insignias.map((insignia) => {
      siguienteId.current += 1;
      return { id: siguienteId.current, insignia };
    });
    setAvisos((actuales) => [...actuales, ...nuevos]);
  }, []);

  const cerrar = useCallback((id) => {
    setAvisos((actuales) => actuales.filter((aviso) => aviso.id !== id));
  }, []);

  return (
    <AvisosInsigniaContexto.Provider value={mostrarInsignias}>
      {children}
      {/* Encima del aviso "¡Meta cumplida!"; en el celular, a todo el ancho. */}
      <div
        role="status"
        aria-live="polite"
        className="pointer-events-none fixed inset-x-4 bottom-44 z-50 flex flex-col gap-2 sm:inset-x-auto sm:bottom-28 sm:right-8 sm:w-96"
      >
        {avisos.map(({ id, insignia }) => (
          <AvisoInsignia key={id} id={id} insignia={insignia} onCerrar={cerrar} />
        ))}
      </div>
    </AvisosInsigniaContexto.Provider>
  );
}
