import { useEffect, useId, useRef, useState } from 'react';
import {
  Ellipsis, Eye, EyeOff, Pencil, Trash2, UserPlus,
} from 'lucide-react';

/**
 * Menú "⋯" de una tarjeta de meta. Muestra solo las acciones que reciba
 * (Editar / Eliminar). Se cierra al elegir una opción, al tocar fuera o con Esc.
 * HU6: si la meta está cumplida, Editar aparece deshabilitado (regla de la HU2).
 * HU25: "Ocultar del perfil" o, si ya está oculta, "Mostrar en el perfil".
 * HU13: "Invitar personas" (solo el creador; deshabilitado si la meta está cumplida).
 */
export default function MenuTarjeta({
  nombre, onEditar, editarBloqueado = false, oculta = false, onCambiarVisibilidad, onEliminar, onInvitar,
}) {
  const [abierto, setAbierto] = useState(false);
  const contenedor = useRef(null);
  const boton = useRef(null);
  const idAyuda = useId();
  const idAyudaInvitar = useId();

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
          className="absolute right-0 top-full z-20 mt-1 flex w-60 flex-col rounded-xl border border-zinc-200 bg-white p-2 shadow-[0_4px_20px_rgba(30,27,46,0.08)]"
        >
          {onEditar && editarBloqueado && (
            <div role="none" className="flex flex-col gap-1 px-3 py-2">
              <button
                type="button"
                role="menuitem"
                disabled
                aria-disabled="true"
                aria-describedby={idAyuda}
                className="flex h-10 cursor-not-allowed items-center gap-3 text-left text-base font-semibold text-gray-400"
              >
                <Pencil className="size-5" aria-hidden="true" />
                Editar
              </button>
              <p id={idAyuda} className="text-sm leading-5 text-gray-500">
                Una meta cumplida no se puede editar.
              </p>
            </div>
          )}
          {onEditar && !editarBloqueado && (
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
          {onInvitar && editarBloqueado && (
            <div role="none" className="flex flex-col gap-1 px-3 py-2">
              <button
                type="button"
                role="menuitem"
                disabled
                aria-disabled="true"
                aria-describedby={idAyudaInvitar}
                className="flex h-10 cursor-not-allowed items-center gap-3 text-left text-base font-semibold text-gray-400"
              >
                <UserPlus className="size-5" aria-hidden="true" />
                Invitar personas
              </button>
              <p id={idAyudaInvitar} className="text-sm leading-5 text-gray-500">
                Solo se puede invitar a metas activas.
              </p>
            </div>
          )}
          {onInvitar && !editarBloqueado && (
            <button
              type="button"
              role="menuitem"
              onClick={() => elegir(onInvitar)}
              className="flex h-12 items-center gap-3 rounded-xl px-3 text-left text-base font-semibold text-gray-900 hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
            >
              <UserPlus className="size-5" aria-hidden="true" />
              Invitar personas
            </button>
          )}
          {onCambiarVisibilidad && (
            <button
              type="button"
              role="menuitem"
              onClick={() => elegir(onCambiarVisibilidad)}
              className="flex h-12 items-center gap-3 rounded-xl px-3 text-left text-base font-semibold text-gray-900 hover:bg-slate-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
            >
              {oculta
                ? <Eye className="size-5" aria-hidden="true" />
                : <EyeOff className="size-5" aria-hidden="true" />}
              {oculta ? 'Mostrar en el perfil' : 'Ocultar del perfil'}
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
