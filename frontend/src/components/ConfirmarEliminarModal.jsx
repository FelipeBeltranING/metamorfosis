
import { CircleAlert, LoaderCircle, Trash2, X } from 'lucide-react';

export default function ConfirmarEliminarModal({
  meta,
  eliminando,
  error,
  onConfirmar,
  onCancelar,
}) {
  const nombreMeta = meta?.titulo || meta?.nombre || 'esta meta';

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !eliminando) {
          onCancelar();
        }
      }}
    >
      <section
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="titulo-confirmar-eliminar"
        aria-describedby="descripcion-confirmar-eliminar"
        className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl"
      >
        <div className="mb-5 flex items-start justify-between">
          <div className="flex size-12 items-center justify-center rounded-xl bg-red-50 text-red-600">
            <Trash2 className="size-6" aria-hidden="true" />
          </div>

          <button
            type="button"
            onClick={onCancelar}
            disabled={eliminando}
            aria-label="Cerrar ventana"
            className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 disabled:opacity-50"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>

        <h2
          id="titulo-confirmar-eliminar"
          className="text-xl font-bold text-gray-900"
        >
          ¿Eliminar esta meta?
        </h2>

        <p
          id="descripcion-confirmar-eliminar"
          className="mt-3 text-sm leading-6 text-gray-600"
        >
          ¿Seguro que deseas eliminar <strong>{nombreMeta}</strong>?
          Esta acción no se puede deshacer.
        </p>

        {error && (
          <div
            role="alert"
            className="mt-4 flex gap-2 rounded-xl bg-red-50 p-3 text-sm text-red-700"
          >
            <CircleAlert
              className="size-5 shrink-0"
              aria-hidden="true"
            />
            <p>{error}</p>
          </div>
        )}

        <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onCancelar}
            disabled={eliminando}
            className="h-11 rounded-xl border border-gray-200 px-5 font-semibold text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            Cancelar
          </button>

          <button
            type="button"
            onClick={onConfirmar}
            disabled={eliminando}
            className="flex h-11 items-center justify-center gap-2 rounded-xl bg-red-600 px-5 font-semibold text-white hover:bg-red-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {eliminando ? (
              <>
                <LoaderCircle
                  className="size-4 animate-spin"
                  aria-hidden="true"
                />
                Eliminando...
              </>
            ) : (
              <>
                <Trash2 className="size-4" aria-hidden="true" />
                Eliminar meta
              </>
            )}
          </button>
        </div>
      </section>
    </div>
  );
}
