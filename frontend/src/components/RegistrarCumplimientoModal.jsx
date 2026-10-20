import { useState } from 'react';
import { CalendarCheck, Check, CircleAlert, LoaderCircle, X, XCircle } from 'lucide-react';
import BotonOpcion from './BotonOpcion';

const HOY = new Date().toISOString().slice(0, 10);

/**
 * HU5: registra el cumplimiento de una meta en una fecha elegida por el usuario
 * (hoy o un día anterior). Misma estructura y estilo que ConfirmarEliminarModal.
 */
export default function RegistrarCumplimientoModal({
  meta,
  guardando,
  error,
  onConfirmar,
  onCancelar,
}) {
  const [fechaFinalizacion, setFechaFinalizacion] = useState(HOY);
  const [cumplimiento, setCumplimiento] = useState(true);
  const [errorFecha, setErrorFecha] = useState('');

  const nombreMeta = meta?.nombre || 'esta meta';

  const manejarCambioFecha = (valor) => {
    setFechaFinalizacion(valor);
    setErrorFecha(valor && valor > HOY ? 'No puedes elegir una fecha futura' : '');
  };

  const confirmar = () => {
    if (!fechaFinalizacion) {
      setErrorFecha('Elige una fecha');
      return;
    }
    if (fechaFinalizacion > HOY) {
      setErrorFecha('No puedes elegir una fecha futura');
      return;
    }
    onConfirmar({ fechaFinalizacion, cumplimiento });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget && !guardando) {
          onCancelar();
        }
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-registrar-cumplimiento"
        aria-describedby="descripcion-registrar-cumplimiento"
        className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl"
      >
        <div className="mb-5 flex items-start justify-between">
          <div className="flex size-12 items-center justify-center rounded-xl bg-violet-50 text-violet-600">
            <CalendarCheck className="size-6" aria-hidden="true" />
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

        <h2
          id="titulo-registrar-cumplimiento"
          className="text-xl font-bold text-gray-900"
        >
          Registrar cumplimiento
        </h2>

        <p
          id="descripcion-registrar-cumplimiento"
          className="mt-3 text-sm leading-6 text-gray-600"
        >
          ¿Cumpliste <strong>{nombreMeta}</strong>? Elige la fecha y marca si la cumpliste o no.
        </p>

        <div className="mt-5 flex flex-col gap-1.5">
          <label htmlFor="fecha-cumplimiento" className="text-sm font-bold text-gray-900">
            Fecha *
          </label>
          <input
            id="fecha-cumplimiento"
            type="date"
            value={fechaFinalizacion}
            max={HOY}
            onChange={(e) => manejarCambioFecha(e.target.value)}
            disabled={guardando}
            aria-required="true"
            aria-invalid={Boolean(errorFecha)}
            aria-describedby={errorFecha ? 'error-fecha-cumplimiento' : undefined}
            className={`h-[46px] w-full rounded-xl border px-4 text-[15px] outline-none focus:ring-2 focus:ring-violet-300 ${
              errorFecha ? 'border-red-400' : 'border-zinc-200'
            } ${guardando ? 'bg-slate-100 text-gray-500' : 'bg-white text-gray-900'}`}
          />
          {errorFecha && (
            <p id="error-fecha-cumplimiento" role="alert" className="text-sm font-semibold text-red-600">
              {errorFecha}
            </p>
          )}
        </div>

        <div className="mt-4 grid grid-cols-2 gap-2.5">
          <BotonOpcion
            grupo="cumplimiento"
            valor="si"
            etiqueta="Sí la cumplí"
            Icono={Check}
            seleccionado={cumplimiento === true}
            deshabilitado={guardando}
            onChange={() => setCumplimiento(true)}
          />
          <BotonOpcion
            grupo="cumplimiento"
            valor="no"
            etiqueta="No la cumplí"
            Icono={XCircle}
            seleccionado={cumplimiento === false}
            deshabilitado={guardando}
            onChange={() => setCumplimiento(false)}
          />
        </div>

        {error && (
          <div
            role="alert"
            className="mt-4 flex gap-2 rounded-xl bg-red-50 p-3 text-sm text-red-700"
          >
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
            type="button"
            onClick={confirmar}
            disabled={guardando || Boolean(errorFecha)}
            className="flex h-11 items-center justify-center gap-2 rounded-xl bg-violet-600 px-5 font-semibold text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {guardando ? (
              <>
                <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />
                Guardando...
              </>
            ) : (
              'Guardar'
            )}
          </button>
        </div>
      </section>
    </div>
  );
}
