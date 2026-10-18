import { useEffect } from 'react';
import { CircleAlert, TriangleAlert } from 'lucide-react';

/**
 * Confirmación para eliminar una meta (HU4). En móvil sube desde abajo; en escritorio va centrada.
 * No se puede cerrar mientras se está eliminando.
 */
export default function ConfirmarEliminarModal({ meta, eliminando, error, onConfirmar, onCancelar }) {
  // Esc cierra el cuadro (salvo que ya se esté eliminando).
  useEffect(() => {
    const alPresionar = (evento) => {
      if (evento.key === 'Escape' && !eliminando) onCancelar();
    };
    document.addEventListener('keydown', alPresionar);
    return () => document.removeEventListener('keydown', alPresionar);
  }, [eliminando, onCancelar]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-gray-900/50 sm:items-center sm:p-4"
      onClick={() => !eliminando && onCancelar()}
    >
      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="eliminar-titulo"
        aria-describedby="eliminar-descripcion"
        onClick={(evento) => evento.stopPropagation()}
        className="flex w-full flex-col gap-6 rounded-t-3xl bg-white p-6 sm:w-96 sm:rounded-3xl sm:p-8"
      >
        <div className="flex size-14 items-center justify-center rounded-2xl bg-red-50">
          <TriangleAlert className="size-7 text-red-600" aria-hidden="true" />
        </div>

        <div className="flex flex-col gap-3">
          <h2 id="eliminar-titulo" className="text-2xl font-bold text-gray-900">
            ¿Eliminar esta meta?
          </h2>
          <p id="eliminar-descripcion" className="text-base text-gray-900">
            Vas a eliminar «<strong className="break-words font-bold">{meta.nombre}</strong>». Se
            borrarán también sus registros de progreso. Esta acción no se puede deshacer.
          </p>
        </div>

        {error && (
          <p
            id="eliminar-error"
            role="alert"
            className="flex items-start gap-2 text-sm font-semibold leading-5 text-red-600"
          >
            <CircleAlert className="size-5 shrink-0" aria-hidden="true" />
            {error}
          </p>
        )}

        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onCancelar}
            disabled={eliminando}
            autoFocus
            className="h-14 w-full rounded-xl border border-zinc-200 bg-white px-6 text-base font-bold text-gray-900 focus:outline-none focus:ring-[3px] focus:ring-inset focus:ring-violet-400 enabled:hover:bg-slate-50 disabled:cursor-not-allowed disabled:bg-gray-100 sm:w-36"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={onConfirmar}
            disabled={eliminando}
            aria-busy={eliminando}
            className="h-14 w-full rounded-xl bg-red-600 px-6 text-base font-bold text-white focus:outline-none focus:ring-2 focus:ring-red-300 enabled:hover:bg-red-700 disabled:cursor-not-allowed disabled:bg-red-800 sm:w-40"
          >
            {eliminando ? 'Eliminando…' : 'Sí, eliminar'}
          </button>
        </div>
      </div>
    </div>
  );
}