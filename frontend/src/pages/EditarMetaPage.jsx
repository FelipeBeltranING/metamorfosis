import { useEffect, useState } from 'react';
import { ArrowLeft, CircleAlert, Info } from 'lucide-react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import Encabezado from '../components/Encabezado';
import BotonOpcion from '../components/BotonOpcion';
import MensajeError from '../components/MensajeError';
import { editarMeta, listarMetas } from '../services/metaService';
import { NOMBRE_MAX, PLAZOS, TIPOS } from '../constants/opcionesMeta';

const FORMULARIO_VACIO = { nombre: '', plazo: '', tipo: '' };

/** Situaciones en las que la meta no se puede editar: se muestra el aviso y se bloquea el formulario. */
const BLOQUEOS = {
  noExiste: { tipo: 'error', texto: 'Esta meta ya no existe' },
  sinPermiso: { tipo: 'error', texto: 'Solo el creador puede editar esta meta' },
  cumplida: { tipo: 'info', texto: 'Esta meta ya está cumplida y no se puede editar' },
};

const desdeMeta = (meta) => ({ nombre: meta.nombre, plazo: meta.plazo, tipo: meta.tipo });

/** Validación del cliente (la del servidor se mantiene siempre). */
function validar({ nombre, plazo, tipo }) {
  const errores = {};
  if (!nombre.trim()) errores.nombre = 'El nombre es obligatorio';
  else if (nombre.trim().length > NOMBRE_MAX) errores.nombre = `Máximo ${NOMBRE_MAX} caracteres`;
  if (!plazo) errores.plazo = 'Elige un plazo';
  if (!tipo) errores.tipo = 'Elige un tipo';
  return errores;
}

/** Traduce la respuesta del servidor a un bloqueo del formulario (o null si es otro error). */
function bloqueoDesde(error) {
  if (error.estado === 404) return 'noExiste';
  if (error.estado === 403) return 'sinPermiso';
  if (error.estado === 400 && error.errores?.some((e) => /cumplida/i.test(e))) return 'cumplida';
  return null;
}

function Aviso({ tipo, texto }) {
  const esInfo = tipo === 'info';
  const Icono = esInfo ? Info : CircleAlert;
  return (
    <div
      role={esInfo ? 'status' : 'alert'}
      className={`flex items-center gap-3 rounded-xl p-4 text-sm font-semibold leading-5 ${
        esInfo
          ? 'bg-slate-100 text-gray-900'
          : 'bg-red-50 text-red-700 shadow-[0_4px_20px_rgba(30,27,46,0.03)]'
      }`}
    >
      <Icono className={`size-6 shrink-0 ${esInfo ? 'text-gray-500' : 'text-red-700'}`} aria-hidden="true" />
      <span className="flex-1">{texto}</span>
    </div>
  );
}

export default function EditarMetaPage() {
  const { codigoMeta: parametro } = useParams();
  const codigoMeta = Number(parametro);
  const codigoValido = Number.isInteger(codigoMeta);
  const location = useLocation();
  const navigate = useNavigate();

  // Si venimos de la lista, la meta llega en el estado de navegación; si no (recarga o enlace
  // directo), se busca en la lista de metas activas.
  const metaInicial = location.state?.meta?.codigoMeta === codigoMeta ? location.state.meta : null;

  const [formulario, setFormulario] = useState(metaInicial ? desdeMeta(metaInicial) : FORMULARIO_VACIO);
  const [cargando, setCargando] = useState(codigoValido && !metaInicial);
  const [bloqueo, setBloqueo] = useState(codigoValido ? null : 'noExiste');
  const [errores, setErrores] = useState({});
  const [errorServidor, setErrorServidor] = useState('');
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (!cargando) return undefined;
    let activo = true;
    listarMetas()
      .then((metas) => {
        if (!activo) return;
        const meta = metas.find((m) => m.codigoMeta === codigoMeta);
        if (meta) setFormulario(desdeMeta(meta));
        else setBloqueo('noExiste');
        setCargando(false);
      })
      .catch((error) => {
        if (!activo) return;
        setErrorServidor(error.message);
        setCargando(false);
      });
    return () => {
      activo = false;
    };
  }, [cargando, codigoMeta]);

  const bloqueado = Boolean(bloqueo) || cargando || enviando;

  const actualizar = (campo, valor) => {
    setFormulario((anterior) => ({ ...anterior, [campo]: valor }));
    setErrores((anterior) => ({ ...anterior, [campo]: undefined }));
  };

  const enviar = async (evento) => {
    evento.preventDefault();
    setErrorServidor('');

    const erroresCliente = validar(formulario);
    setErrores(erroresCliente);
    if (Object.keys(erroresCliente).length > 0) return;

    setEnviando(true);
    try {
      await editarMeta(codigoMeta, {
        nombre: formulario.nombre.trim(),
        plazo: formulario.plazo,
        tipo: formulario.tipo,
      });
      navigate('/metas', { state: { metaActualizada: true } });
    } catch (error) {
      const causa = bloqueoDesde(error);
      if (causa) {
        setBloqueo(causa);
      } else {
        const detalle = error.errores?.length ? `: ${error.errores.join(', ')}` : '';
        setErrorServidor(`${error.message}${detalle}`);
      }
    } finally {
      setEnviando(false);
    }
  };

  const avisoVisible = bloqueo ? BLOQUEOS[bloqueo] : errorServidor && { tipo: 'error', texto: errorServidor };

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />

      <main className="mx-auto w-full max-w-[640px] px-4 pb-10 pt-6 sm:pt-12">
        <form
          onSubmit={enviar}
          noValidate
          aria-busy={cargando}
          className="flex flex-col gap-6 rounded-2xl border border-zinc-200 bg-white p-5 shadow-[0_4px_20px_rgba(30,27,46,0.03)] sm:p-8"
        >
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => navigate('/metas')}
              aria-label="Volver a mis metas"
              className="flex size-12 items-center justify-center rounded-xl hover:bg-slate-100"
            >
              <ArrowLeft className="size-6" aria-hidden="true" />
            </button>
            <h1 className="text-2xl font-bold sm:text-3xl">Editar meta</h1>
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
              disabled={bloqueado}
              onChange={(e) => actualizar('nombre', e.target.value)}
              placeholder="Ej. Leer 20 libros"
              aria-invalid={Boolean(errores.nombre)}
              aria-describedby={errores.nombre ? 'error-nombre' : undefined}
              className={`h-14 w-full rounded-xl border p-4 text-base font-semibold outline-none placeholder:font-normal placeholder:text-gray-500 focus:ring-2 focus:ring-violet-300 disabled:cursor-not-allowed disabled:bg-slate-100 ${
                errores.nombre ? 'border-red-500' : 'border-zinc-200'
              } bg-white`}
            />
            <MensajeError id="error-nombre" texto={errores.nombre} />
          </div>

          {/* Plazo */}
          <fieldset className="flex flex-col gap-2" disabled={bloqueado}>
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
                  deshabilitado={bloqueado}
                  onChange={(v) => actualizar('plazo', v)}
                />
              ))}
            </div>
            <MensajeError texto={errores.plazo} />
          </fieldset>

          {/* Tipo */}
          <fieldset className="flex flex-col gap-2" disabled={bloqueado}>
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
                  deshabilitado={bloqueado}
                  onChange={(v) => actualizar('tipo', v)}
                />
              ))}
            </div>
            <MensajeError texto={errores.tipo} />
          </fieldset>

          {avisoVisible && <Aviso tipo={avisoVisible.tipo} texto={avisoVisible.texto} />}

          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:items-center sm:justify-end">
            <button
              type="button"
              onClick={() => navigate('/metas')}
              className="h-12 rounded-xl px-6 text-base font-bold text-gray-500 hover:bg-slate-100"
            >
              {bloqueo === 'noExiste' ? 'Volver a mis metas' : 'Cancelar'}
            </button>
            <button
              type="submit"
              disabled={bloqueado}
              className="h-14 rounded-xl bg-violet-600 px-6 text-base font-bold text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:bg-violet-600 sm:w-52"
            >
              {enviando ? 'Guardando...' : 'Guardar cambios'}
            </button>
          </div>
        </form>
      </main>
    </div>
  );
}
