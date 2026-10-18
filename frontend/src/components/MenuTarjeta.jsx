import { useEffect, useRef, useState } from 'react';
import { Ellipsis, Pencil, Trash2 } from 'lucide-react';

/**
 * Menú "⋯" de una tarjeta de meta. Muestra solo las acciones que reciba (Editar / Eliminar).
 * Se cierra al elegir una opción, al tocar fuera o con Esc.
 */
export default function MenuTarjeta({ nombre, onEditar, onEliminar }) {
  const [abierto, setAbierto] = useState(false);
  const contenedor = useRef(null);
  const boton = useRef(null);

  useEffect(() => {
    if (!abierto) return undefined;
    const alTocarFuera = (evento) => {
      if (!contenedor.current?.contains(evento.target)) setAbierto(false);
    };
    const alPresionarTecla = (evento) => {
      if (evento.key === 'Escape') {
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

  const elegir = (accion) => {
    setAbierto(false);
    accion();
  };

  return (
    <div ref={contenedor} className="relative">
      <button
        ref={boton}
        type="button"
        onClick={() => setAbierto((actual) => !actual)}
        aria-haspopup="menu"
        aria-expanded={abierto}
        aria-label={`Opciones de la meta ${nombre}`}
        className="flex size-12 items-center justify-center rounded-xl text-gray-900 transition hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
      >
        <Ellipsis className="size-6" aria-hidden="true" />
      </button>

      {abierto && (
        <div
          role="menu"
          className="absolute right-0 top-full z-20 mt-1 flex w-44 flex-col rounded-xl border border-zinc-200 bg-white p-2 shadow-[0_4px_20px_rgba(30,27,46,0.08)]"
        >
          {onEditar && (
            <button
              type="button"
              role="menuitem"
              onClick={() => elegir(onEditar)}
              className="flex h-12 items-center gap-3 rounded-xl px-3 text-left text-base font-semibold text-gray-900 hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
            >
              <Pencil className="size-5" aria-hidden="true" />
              Editar
            </button>
          )}
          {onEliminar && (
            <button
              type="button"
              role="menuitem"
              onClick={() => elegir(onEliminar)}
              className="flex h-12 items-center gap-3 rounded-xl px-3 text-left text-base font-semibold text-red-600 hover:bg-red-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-300"
            >
              <Trash2 className="size-5" aria-hidden="true" />
              Eliminar
            </button>
          )}
        </div>
      )}
    </div>
  );
}
