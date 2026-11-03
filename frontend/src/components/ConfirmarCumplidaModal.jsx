import { useEffect, useRef } from 'react';
import { CircleAlert, CircleCheck, LoaderCircle, X } from 'lucide-react';

/**
 * HU6: confirma antes de marcar una meta como cumplida, porque no se puede deshacer.
 * Se cierra con Cancelar, la X, Esc o tocando fuera (salvo mientras guarda).
 */
export default function ConfirmarCumplidaModal({
  meta, guardando, error, onConfirmar, onCancelar,
}) {
  const botonConfirmar = useRef(null);

  // Foco en la acción principal al abrir; Esc cierra.
  useEffect(() => {
    botonConfirmar.current?.focus();
    const alPresionarTecla = (evento) => {
      if (evento.key === 'Escape' && !guardando) onCancelar();
    };
    document.addEventListener('keydown', alPresionarTecla);
    return () => document.removeEventListener('keydown', alPresionarTecla);
  }, [guardando, onCancelar]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !guardando) onCancelar();
      }}
    >
      <section
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="titulo-confirmar-cumplida"
        aria-describedby="descripcion-confirmar-cumplida"
        className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl"
      >
        <div className="mb-5 flex items-start justify-between">
          <div className="flex size-12 items-center justify-center rounded-xl bg-emerald-50 text-emerald-600">
            <CircleCheck className="size-6" aria-hidden="true" />
          </div>
          <button
            type="button"
            onClick={onCancelar}
            disabled={guardando}
            aria-label="Cerrar ventana"
            className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 disabled:opacity-50"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>

        <h2 id="titulo-confirmar-cumplida" className="text-xl font-bold text-gray-900">
          ¿Marcar esta meta como cumplida?
        </h2>
        <p id="descripcion-confirmar-cumplida" className="mt-3 text-sm leading-6 text-gray-600">
          <strong>{meta.nombre}</strong> quedará como cumplida con la fecha de hoy y pasará a tus logros.
          Ya no podrás editarla ni agregarle sub-metas, y sus sub-metas dejarán de registrarse; su
          historial y estadísticas se conservan. Esta acción no se puede deshacer.
        </p>

        {error && (
          <div role="alert" className="mt-4 flex gap-2 rounded-xl bg-red-50 p-3 text-sm text-red-700">
            <CircleAlert className="size-5 shrink-0" aria-hidden="true" />
            <p>{error}</p>
          </div>
        )}

        <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onCancelar}
            disabled={guardando}
            className="h-11 rounded-xl border border-gray-200 px-5 font-semibold text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            Cancelar
          </button>
          <button
            ref={botonConfirmar}
            type="button"
            onClick={onConfirmar}
            disabled={guardando}
            className="flex h-11 items-center justify-center gap-2 rounded-xl bg-emerald-600 px-5 font-semibold text-white hover:bg-emerald-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-300 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {guardando ? (
              <>
                <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                Marcando...
              </>
            ) : (
              <>
                <CircleCheck className="size-4" aria-hidden="true" />
                Marcar como cumplida
              </>
            )}
          </button>
        </div>
      </section>
    </div>
  );
}
