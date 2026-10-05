import { useEffect, useState } from 'react';
import { ArrowLeft, CircleCheck } from 'lucide-react';
import Encabezado from '../components/Encabezado';
import BotonOpcion from '../components/BotonOpcion';
import MensajeError from '../components/MensajeError';
import { crearMeta } from '../services/metaService';
import { NOMBRE_MAX, PLAZOS, TIPOS } from '../constants/opcionesMeta';

const FORMULARIO_VACIO = { nombre: '', plazo: '', tipo: '' };

/** Validación del cliente (la del servidor se mantiene siempre). */
function validar({ nombre, plazo, tipo }) {
  const errores = {};
  if (!nombre.trim()) errores.nombre = 'El nombre es obligatorio';
  else if (nombre.trim().length > NOMBRE_MAX) errores.nombre = `Máximo ${NOMBRE_MAX} caracteres`;
  if (!plazo) errores.plazo = 'Elige un plazo';
  if (!tipo) errores.tipo = 'Elige un tipo';
  return errores;
}

export default function CrearMetaPage() {
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO);
  const [errores, setErrores] = useState({});
  const [errorServidor, setErrorServidor] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [metaCreada, setMetaCreada] = useState(false);

  // Oculta la notificación de éxito a los 4 segundos.
  useEffect(() => {
    if (!metaCreada) return undefined;
    const temporizador = setTimeout(() => setMetaCreada(false), 4000);
    return () => clearTimeout(temporizador);
  }, [metaCreada]);

  const actualizar = (campo, valor) => {
    setFormulario((anterior) => ({ ...anterior, [campo]: valor }));
    setErrores((anterior) => ({ ...anterior, [campo]: undefined }));
  };

  const limpiar = () => {
    setFormulario(FORMULARIO_VACIO);
    setErrores({});
    setErrorServidor('');
  };

  const enviar = async (evento) => {
    evento.preventDefault();
    setErrorServidor('');

    const erroresCliente = validar(formulario);
    setErrores(erroresCliente);
    if (Object.keys(erroresCliente).length > 0) return;

    setEnviando(true);
    try {
      await crearMeta({
        nombre: formulario.nombre.trim(),
        plazo: formulario.plazo,
        tipo: formulario.tipo,
      });
      limpiar();
      setMetaCreada(true);
    } catch (error) {
      const detalle = error.errores?.length ? `: ${error.errores.join(', ')}` : '';
      setErrorServidor(`${error.message}${detalle}`);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />

      <main className="mx-auto w-full max-w-[640px] px-4 pb-10 pt-6 sm:pt-12">
        <form
          onSubmit={enviar}
          noValidate
          className="flex flex-col gap-6 rounded-2xl border border-zinc-200 bg-white p-5 shadow-[0_4px_20px_rgba(30,27,46,0.03)] sm:p-8"
        >
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => window.history.back()}
              aria-label="Volver"
              className="flex size-12 items-center justify-center rounded-xl hover:bg-slate-100"
            >
              <ArrowLeft className="size-6" aria-hidden="true" />
            </button>
            <h1 className="text-2xl font-bold sm:text-3xl">Crear meta</h1>
          </div>

          {/* Nombre */}
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between">
              <label htmlFor="nombre" className="text-base font-bold">Nombre de la meta *</label>
              <span className="text-xs font-semibold text-gray-500">
                {formulario.nombre.length}/{NOMBRE_MAX}
              </span>
            </div>
            <input
              id="nombre"
              type="text"
              maxLength={NOMBRE_MAX}
              value={formulario.nombre}
              onChange={(e) => actualizar('nombre', e.target.value)}
              placeholder="Ej. Leer 20 libros"
              aria-invalid={Boolean(errores.nombre)}
              aria-describedby={errores.nombre ? 'error-nombre' : undefined}
              className={`h-14 w-full rounded-xl border bg-white p-4 text-base outline-none placeholder:text-gray-500 focus:ring-2 focus:ring-violet-300 ${
                errores.nombre ? 'border-red-500' : 'border-zinc-200'
              }`}
            />
            <MensajeError id="error-nombre" texto={errores.nombre} />
          </div>

          {/* Plazo */}
          <fieldset className="flex flex-col gap-2">
            <legend className="mb-2 text-base font-bold">Plazo *</legend>
            <div className="grid grid-cols-3 gap-2">
              {PLAZOS.map(({ valor, etiqueta }) => (
                <BotonOpcion
                  key={valor}
                  vertical
                  grupo="plazo"
                  valor={valor}
                  etiqueta={etiqueta}
                  seleccionado={formulario.plazo === valor}
                  conError={Boolean(errores.plazo)}
                  onChange={(v) => actualizar('plazo', v)}
                />
              ))}
            </div>
            <MensajeError texto={errores.plazo} />
          </fieldset>

          {/* Tipo */}
          <fieldset className="flex flex-col gap-2">
            <legend className="mb-2 text-base font-bold">Tipo de meta *</legend>
            <div className="grid grid-cols-2 gap-2 md:grid-cols-4">
              {TIPOS.map(({ valor, etiqueta, Icono }) => (
                <BotonOpcion
                  key={valor}
                  grupo="tipo"
                  valor={valor}
                  etiqueta={etiqueta}
                  Icono={Icono}
                  seleccionado={formulario.tipo === valor}
                  conError={Boolean(errores.tipo)}
                  onChange={(v) => actualizar('tipo', v)}
                />
              ))}
            </div>
            <MensajeError texto={errores.tipo} />
          </fieldset>

          {errorServidor && (
            <p role="alert" className="rounded-xl bg-red-50 p-3 text-sm font-semibold text-red-700">
              {errorServidor}
            </p>
          )}

          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:items-center sm:justify-end">
            <button
              type="button"
              onClick={limpiar}
              className="h-12 rounded-xl px-6 text-base font-bold text-gray-500 hover:bg-slate-100"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={enviando}
              className="h-14 rounded-xl bg-violet-600 px-6 text-base font-bold text-white hover:bg-violet-700 disabled:opacity-60"
            >
              {enviando ? 'Creando...' : 'Crear meta'}
            </button>
          </div>
        </form>
      </main>

      {metaCreada && (
        <div
          role="status"
          className="fixed bottom-6 left-1/2 flex w-[calc(100%-2rem)] max-w-md -translate-x-1/2 items-center gap-3 rounded-xl bg-emerald-400 p-4 text-sm font-semibold text-gray-900 shadow-lg"
        >
          <CircleCheck className="size-6 shrink-0" aria-hidden="true" />
          ¡Meta creada! Ya aparece en tus metas activas.
        </div>
      )}
    </div>
  );
}