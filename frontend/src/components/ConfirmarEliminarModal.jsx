import { useEffect } from 'react';
import { TriangleAlert } from 'lucide-react';
import MensajeError from './MensajeError';

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
        className="flex w-full max-w-md flex-col gap-5 rounded-t-3xl bg-white p-6 shadow-xl sm:rounded-3xl sm:p-8"
      >
        <div className="flex size-14 items-center justify-center rounded-2xl bg-red-50">
          <TriangleAlert className="size-7 text-red-600" aria-hidden="true" />
        </div>

        <div className="flex flex-col gap-2">
          <h2 id="eliminar-titulo" className="text-xl font-bold text-gray-900 sm:text-2xl">
            ¿Eliminar esta meta?
          </h2>
          <p id="eliminar-descripcion" className="text-base text-gray-500">
            Vas a eliminar{' '}
            <strong className="break-words font-bold text-gray-900">«{meta.nombre}»</strong>. Se
            borrarán también sus registros de progreso. Esta acción no se puede deshacer.
          </p>
        </div>

        <MensajeError id="eliminar-error" texto={error} />

        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onCancelar}
            disabled={eliminando}
            autoFocus
            className="h-14 rounded-xl border border-zinc-200 bg-white px-6 text-base font-bold text-gray-900 hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300 disabled:cursor-not-allowed disabled:opacity-50"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={onConfirmar}
            disabled={eliminando}
            aria-busy={eliminando}
            className="h-14 rounded-xl bg-red-600 px-6 text-base font-bold text-white hover:bg-red-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-300 disabled:cursor-not-allowed disabled:opacity-70"
          >
            {eliminando ? 'Eliminando…' : 'Sí, eliminar'}
          </button>
        </div>
      </div>
    </div>
  );
}
