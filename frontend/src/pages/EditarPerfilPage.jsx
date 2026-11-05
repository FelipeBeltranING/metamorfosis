import { useEffect, useState } from 'react';
import {
  ArrowLeft, CircleAlert, CircleCheck, LoaderCircle, Lock,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import Encabezado from '../components/Encabezado';
import MensajeError from '../components/MensajeError';
import { editarDatos, obtenerCuenta } from '../services/cuentaService';
import { actualizarSesion } from '../services/sesionService';

/** Igual que el servidor (CuentaService.TEXTO_MAXIMO). */
export const TEXTO_MAX = 50;

const limpiar = (texto) => texto.trim().replace(/\s+/g, ' ');

/** Validación del cliente (la del servidor se mantiene siempre). */
function validar({ nombre, apellido }) {
  const errores = {};
  for (const [campo, valor] of [['nombre', nombre], ['apellido', apellido]]) {
    const limpio = limpiar(valor);
    if (!limpio) errores[campo] = `Escribe tu ${campo}.`;
    else if (limpio.length > TEXTO_MAX) errores[campo] = `Máximo ${TEXTO_MAX} caracteres (llevas ${limpio.length}).`;
  }
  return errores;
}

/** Reparte los mensajes del servidor en sus campos; los que no son de un campo van arriba. */
function erroresDelServidor(error) {
  const campos = {};
  const generales = [];
  for (const mensaje of error.errores ?? []) {
    if (/apellido/i.test(mensaje)) campos.apellido = mensaje;
    else if (/nombre/i.test(mensaje)) campos.nombre = mensaje;
    else if (/foto/i.test(mensaje)) campos.foto = mensaje;
    else generales.push(mensaje);
  }
  return { campos, general: generales[0] ?? (Object.keys(campos).length ? '' : error.message) };
}

function CampoPerfil({
  id, etiqueta, valor, onChange, error, maxLength, autoComplete,
}) {
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-bold text-gray-700">{etiqueta}</label>
      <input
        id={id}
        value={valor}
        onChange={(e) => onChange(e.target.value)}
        maxLength={maxLength}
        autoComplete={autoComplete}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `error-${id}` : undefined}
        className={`h-12 w-full rounded-xl border bg-white px-3.5 text-base text-gray-900 outline-none focus:ring-2 focus:ring-violet-300 ${
          error ? 'border-2 border-red-500' : 'border-zinc-300'
        }`}
      />
      <MensajeError id={`error-${id}`} texto={error} />
    </div>
  );
}

/** HU-23: editar nombre, apellido y foto de la cuenta. El correo se muestra, no se edita. */
export default function EditarPerfilPage() {
  const navigate = useNavigate();
  const [carga, setCarga] = useState({ fase: 'cargando' });
  const [intento, setIntento] = useState(0);
  const [formulario, setFormulario] = useState({ nombre: '', apellido: '' });
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [guardado, setGuardado] = useState(false);

  useEffect(() => {
    let activo = true;
    obtenerCuenta()
      .then((cuenta) => {
        if (!activo) return;
        setCarga({ fase: 'lista', cuenta });
        setFormulario({ nombre: cuenta.nombre, apellido: cuenta.apellido });
      })
      .catch((error) => {
        if (!activo) return;
        if (error.estado === 401) navigate('/login', { replace: true });
        else setCarga({ fase: 'error', mensaje: error.message });
      });
    return () => {
      activo = false;
    };
  }, [intento, navigate]);

  // El aviso de "Perfil actualizado" se oculta solo.
  useEffect(() => {
    if (!guardado) return undefined;
    const temporizador = setTimeout(() => setGuardado(false), 4000);
    return () => clearTimeout(temporizador);
  }, [guardado]);

  const cuenta = carga.cuenta;
  const datosCambiaron = Boolean(cuenta) && (
    limpiar(formulario.nombre) !== cuenta.nombre || limpiar(formulario.apellido) !== cuenta.apellido
  );
  const hayCambios = datosCambiaron;

  const cambiar = (campo) => (valor) => {
    setFormulario((actual) => ({ ...actual, [campo]: valor }));
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }));
    setGuardado(false);
  };

  const guardar = async (evento) => {
    evento.preventDefault();
    const encontrados = validar(formulario);
    setErrores(encontrados);
    setErrorGeneral(Object.keys(encontrados).length ? 'No se guardaron los cambios. Revisa los campos marcados.' : '');
    if (Object.keys(encontrados).length || !hayCambios) return;

    setGuardando(true);
    try {
      let actualizada = cuenta;
      if (datosCambiaron) {
        actualizada = await editarDatos({ nombre: limpiar(formulario.nombre), apellido: limpiar(formulario.apellido) });
      }
      setCarga({ fase: 'lista', cuenta: actualizada });
      setFormulario({ nombre: actualizada.nombre, apellido: actualizada.apellido });
      actualizarSesion({
        nombre: actualizada.nombre, apellido: actualizada.apellido, email: actualizada.email, foto: actualizada.foto ?? null,
      });
      setGuardado(true);
    } catch (error) {
      if (error.estado === 401) {
        navigate('/login', { replace: true });
        return;
      }
      const { campos, general } = erroresDelServidor(error);
      setErrores(campos);
      setErrorGeneral(general || 'No se guardaron los cambios. Revisa los campos marcados.');
    } finally {
      setGuardando(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />
      <main className="mx-auto flex max-w-[720px] flex-col gap-5 px-4 pb-12 pt-6 sm:pt-10">
        <Link to="/metas" className="inline-flex items-center gap-1 self-start text-[15px] font-bold text-violet-700 hover:underline">
          <ArrowLeft className="size-[18px]" aria-hidden="true" />
          Volver
        </Link>
        <div>
          <h1 className="text-[26px] font-extrabold sm:text-3xl">Editar perfil</h1>
          <p className="mt-1 text-[15px] text-gray-600 sm:text-base">Así te verán en Metamorfosis y en las metas que compartes.</p>
        </div>

        {carga.fase === 'cargando' && (
          <div className="flex flex-col gap-5" aria-busy="true" aria-label="Cargando tu perfil">
            <div className="h-40 animate-pulse rounded-2xl bg-white" />
            <div className="h-64 animate-pulse rounded-2xl bg-white" />
          </div>
        )}

        {carga.fase === 'error' && (
          <div role="alert" className="flex flex-col gap-4 rounded-2xl bg-red-50 p-5 text-red-700 sm:flex-row sm:items-center sm:p-6">
            <CircleAlert className="size-6 shrink-0" aria-hidden="true" />
            <div className="flex flex-1 flex-col gap-1">
              <p className="font-bold">No pudimos cargar tu perfil</p>
              <p>{carga.mensaje || 'Comprueba tu conexión e inténtalo de nuevo.'}</p>
            </div>
            <button
              type="button"
              onClick={() => { setCarga({ fase: 'cargando' }); setIntento((n) => n + 1); }}
              className="h-11 rounded-xl bg-white px-5 font-bold text-red-700 hover:bg-red-100"
            >
              Reintentar
            </button>
          </div>
        )}

        {carga.fase === 'lista' && (
          <form onSubmit={guardar} noValidate className="flex flex-col gap-5">
            {errorGeneral && (
              <p role="alert" className="flex items-start gap-2.5 rounded-xl border border-red-200 bg-red-50 px-4 py-3.5 text-[15px] text-red-800">
                <CircleAlert className="mt-0.5 size-5 shrink-0" aria-hidden="true" />
                <span>{errorGeneral}</span>
              </p>
            )}

            <section aria-labelledby="titulo-datos" className="flex flex-col gap-4 rounded-2xl border border-zinc-200 bg-white p-5 sm:p-7">
              <h2 id="titulo-datos" className="text-lg font-extrabold">Tus datos</h2>
              <div className="grid gap-4 sm:grid-cols-2">
                <CampoPerfil
                  id="nombre"
                  etiqueta="Nombre"
                  valor={formulario.nombre}
                  onChange={cambiar('nombre')}
                  error={errores.nombre}
                  autoComplete="given-name"
                />
                <CampoPerfil
                  id="apellido"
                  etiqueta="Apellido"
                  valor={formulario.apellido}
                  onChange={cambiar('apellido')}
                  error={errores.apellido}
                  autoComplete="family-name"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label htmlFor="correo" className="text-sm font-bold text-gray-700">Correo</label>
                <div className="relative">
                  <input
                    id="correo"
                    value={cuenta.email}
                    readOnly
                    aria-describedby="ayuda-correo"
                    className="h-12 w-full rounded-xl border border-zinc-200 bg-slate-100 pl-3.5 pr-11 text-base text-gray-600 outline-none"
                  />
                  <Lock className="absolute right-3.5 top-1/2 size-[18px] -translate-y-1/2 text-gray-500" aria-hidden="true" />
                </div>
                <p id="ayuda-correo" className="text-[13px] text-gray-600">Es el correo con el que inicias sesión, por eso no se cambia aquí.</p>
              </div>
            </section>

            <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <Link
                to="/metas"
                className="flex h-12 items-center justify-center rounded-xl border border-zinc-200 bg-white px-6 font-bold text-gray-700 hover:bg-gray-50"
              >
                Cancelar
              </Link>
              <button
                type="submit"
                disabled={guardando || !hayCambios}
                className="flex h-12 items-center justify-center gap-2 rounded-xl bg-violet-600 px-6 font-bold text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {guardando && <LoaderCircle className="size-5 animate-spin" aria-hidden="true" />}
                {guardando ? 'Guardando...' : 'Guardar cambios'}
              </button>
            </div>
          </form>
        )}
      </main>

      {guardado && (
        <p
          role="status"
          className="fixed bottom-6 left-1/2 z-40 flex w-[calc(100%-2rem)] max-w-md -translate-x-1/2 items-center gap-2.5 rounded-xl bg-emerald-400 px-5 py-3.5 text-[15px] font-bold text-emerald-950 shadow-lg"
        >
          <CircleCheck className="size-6 shrink-0" aria-hidden="true" />
          ¡Perfil actualizado!
        </p>
      )}
    </div>
  );
}
