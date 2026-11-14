import {
  useCallback, useEffect, useRef, useState,
} from 'react';
import {
  ArrowLeft, Camera, CircleAlert, CircleCheck, LoaderCircle, Lock, Trash2,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import i18n, { localeActual } from '../i18n';
import Encabezado from '../components/Encabezado';
import MensajeError from '../components/MensajeError';
import AvatarUsuario from '../components/AvatarUsuario';
import FotoPerfilModal from '../components/FotoPerfilModal';
import SeccionApariencia from '../components/SeccionApariencia';
import SeccionPerfilPublico from '../components/SeccionPerfilPublico';
import SeccionMascota from '../components/SeccionMascota';
import { editarPerfilPublico, obtenerPerfil } from '../services/perfilService';
import { listarMetas } from '../services/metaService';
import { MAXIMO_FRASE } from '../constants/portadas';
import {
  cambiarApariencia, cambiarFoto, cambiarMascota, editarDatos, obtenerCuenta, quitarFoto,
} from '../services/cuentaService';
import {
  aplicarApariencia, desdeServidor, haciaServidor, leerApariencia, mismaApariencia,
} from '../tema/apariencia';
import { actualizarSesion, inicialesDe } from '../services/sesionService';

/** Igual que el servidor (CuentaService.TEXTO_MAXIMO). */
const TEXTO_MAX = 50;

const limpiar = (texto) => texto.trim().replace(/\s+/g, ' ');

/** Foto que se puede elegir (se revisa antes de abrir la vista previa). */
const TIPOS_FOTO = ['image/jpeg', 'image/png', 'image/webp'];
const FOTO_MAX_MB = 2;

/** Mensaje de error si el archivo no sirve como foto, o null. */
function problemaConArchivo(archivo) {
  const ayuda = i18n.t('editarPerfil.fotoAyuda', { max: FOTO_MAX_MB });
  if (!TIPOS_FOTO.includes(archivo.type)) return i18n.t('editarPerfil.fotoNoValida', { archivo: archivo.name, ayuda });
  if (archivo.size > FOTO_MAX_MB * 1024 * 1024) {
    const megas = (archivo.size / (1024 * 1024)).toLocaleString(localeActual(), { maximumFractionDigits: 1 });
    return i18n.t('editarPerfil.fotoPesada', { megas, ayuda });
  }
  return null;
}

/** HU19: lo que se edita del perfil público, a partir de Mi perfil. */
function publicoDesde(miPerfil) {
  const p = miPerfil?.personalizacion ?? {};
  return {
    frase: p.frase ?? '',
    portada: p.portada ?? 'NINGUNA',
    codigoInsigniaDestacada: p.insigniaDestacada?.codigoInsignia ?? null,
    metasDestacadas: (p.metasDestacadas ?? []).map((m) => m.codigoMeta),
  };
}

function mismoPerfilPublico(a, b) {
  return limpiar(a.frase) === limpiar(b.frase) && a.portada === b.portada
    && a.codigoInsigniaDestacada === b.codigoInsigniaDestacada
    && [...a.metasDestacadas].sort().join() === [...b.metasDestacadas].sort().join();
}

function haciaServidorPerfil(p) {
  return {
    frase: limpiar(p.frase),
    portada: p.portada,
    codigoInsigniaDestacada: p.codigoInsigniaDestacada,
    metasDestacadas: p.metasDestacadas,
  };
}

/** Validación del cliente (la del servidor se mantiene siempre). */
function validar({ nombre, apellido }) {
  const errores = {};
  for (const [campo, valor] of [['nombre', nombre], ['apellido', apellido]]) {
    const limpio = limpiar(valor);
    if (!limpio) errores[campo] = i18n.t(campo === 'nombre' ? 'editarPerfil.escribeNombre' : 'editarPerfil.escribeApellido');
    else if (limpio.length > TEXTO_MAX) errores[campo] = i18n.t('editarPerfil.maximo', { max: TEXTO_MAX, largo: limpio.length });
  }
  return errores;
}

/** Reparte los mensajes del servidor en sus campos; los que no son de un campo van arriba. */
function erroresDelServidor(error) {
  const campos = {};
  const generales = [];
  for (const mensaje of error.errores ?? []) {
    if (/frase|quote|motto/i.test(mensaje)) campos.frase = mensaje; // HU19: perfil público
    else if (/portada|cover/i.test(mensaje)) campos.portada = mensaje;
    else if (/insignia|badge/i.test(mensaje)) campos.insigniaDestacada = mensaje;
    else if (/destacar|feature/i.test(mensaje)) campos.metasDestacadas = mensaje;
    else if (/apellido|last name/i.test(mensaje)) campos.apellido = mensaje;
    else if (/nombre|name/i.test(mensaje)) campos.nombre = mensaje;
    else if (/foto|photo/i.test(mensaje)) campos.foto = mensaje;
    else if (/modo|acento|mode|accent/i.test(mensaje)) campos.apariencia = mensaje;
    else if (/mascota|pet/i.test(mensaje)) campos.mascota = mensaje;
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

/** HU-23: editar nombre, apellido y foto de la cuenta. El correo se muestra, no se edita. HU19: apariencia. */
export default function EditarPerfilPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [carga, setCarga] = useState({ fase: 'cargando' });
  const [intento, setIntento] = useState(0);
  const [formulario, setFormulario] = useState({ nombre: '', apellido: '' });
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [guardado, setGuardado] = useState(false);
  // Foto pendiente de guardar: null (sin cambios), { tipo: 'nueva', foto } o { tipo: 'quitar' }.
  const [fotoPendiente, setFotoPendiente] = useState(null);
  const [archivoElegido, setArchivoElegido] = useState(null);
  // HU19: apariencia elegida en la página (se ve al instante; se guarda con el formulario).
  const [apariencia, setApariencia] = useState(leerApariencia);
  // HU19: lo que los demás ven del perfil (se guarda con el formulario).
  const [perfil, setPerfil] = useState(null);
  const [perfilPublico, setPerfilPublico] = useState({
    frase: '', portada: 'NINGUNA', codigoInsigniaDestacada: null, metasDestacadas: [],
  });
  const [metasVisibles, setMetasVisibles] = useState([]);
  // HU20: mascota elegida (gratis; se guarda con el formulario).
  const [mascota, setMascota] = useState('MARIPOSA');
  const selectorArchivo = useRef(null);

  useEffect(() => {
    let activo = true;
    Promise.all([obtenerCuenta(), obtenerPerfil(), listarMetas({ estado: 'TODAS' })])
      .then(([cuenta, miPerfil, metas]) => {
        if (!activo) return;
        setPerfil(miPerfil);
        setMetasVisibles(metas.filter((m) => !m.oculta)); // HU25: las ocultas no se destacan
        setPerfilPublico(publicoDesde(miPerfil));
        setCarga({ fase: 'lista', cuenta });
        setFormulario({ nombre: cuenta.nombre, apellido: cuenta.apellido });
        setMascota(cuenta.mascota ?? 'MARIPOSA');
        const guardada = desdeServidor(cuenta.apariencia);
        setApariencia(guardada);
        aplicarApariencia(guardada, true);
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

  // HU19: al salir sin guardar, vuelve la apariencia guardada (la vista previa no se queda).
  useEffect(() => () => aplicarApariencia(leerApariencia()), []);

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
  const aparienciaCambio = Boolean(cuenta) && !mismaApariencia(apariencia, desdeServidor(cuenta.apariencia));
  const perfilPublicoCambio = Boolean(perfil) && !mismoPerfilPublico(perfilPublico, publicoDesde(perfil));
  const mascotaCambio = Boolean(cuenta) && mascota !== (cuenta.mascota ?? 'MARIPOSA');
  const hayCambios = datosCambiaron || fotoPendiente !== null || aparienciaCambio || perfilPublicoCambio || mascotaCambio;
  const fotoMostrada = fotoPendiente?.tipo === 'nueva' ? fotoPendiente.foto
    : fotoPendiente?.tipo === 'quitar' ? null : cuenta?.foto ?? null;

  const elegirArchivo = () => {
    setErrores((actuales) => ({ ...actuales, foto: undefined }));
    selectorArchivo.current?.click();
  };

  const alElegirArchivo = (evento) => {
    const archivo = evento.target.files?.[0];
    evento.target.value = ''; // así se puede volver a elegir el mismo archivo
    if (!archivo) return;
    const problema = problemaConArchivo(archivo);
    if (problema) {
      setErrores((actuales) => ({ ...actuales, foto: problema }));
      return;
    }
    setArchivoElegido(archivo);
  };

  const alFallarFoto = useCallback((mensaje) => {
    setArchivoElegido(null);
    setErrores((actuales) => ({ ...actuales, foto: mensaje }));
  }, []);

  const usarFoto = (foto) => {
    setArchivoElegido(null);
    setFotoPendiente({ tipo: 'nueva', foto });
    setGuardado(false);
  };

  const quitarLaFoto = () => {
    setErrores((actuales) => ({ ...actuales, foto: undefined }));
    // Si la foto era solo una elegida sin guardar, quitarla es volver a como estaba.
    setFotoPendiente(cuenta?.foto ? { tipo: 'quitar' } : null);
    setGuardado(false);
  };

  const elegirApariencia = (nueva) => {
    setApariencia(nueva);
    aplicarApariencia(nueva);
    setErrores((actuales) => ({ ...actuales, apariencia: undefined }));
    setGuardado(false);
  };

  const cambiar = (campo) => (valor) => {
    setFormulario((actual) => ({ ...actual, [campo]: valor }));
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }));
    setGuardado(false);
  };

  const guardar = async (evento) => {
    evento.preventDefault();
    const encontrados = validar(formulario);
    if (limpiar(perfilPublico.frase).length > MAXIMO_FRASE) {
      encontrados.frase = t('editarPerfil.fraseLarga', { max: MAXIMO_FRASE });
    }
    setErrores((actuales) => ({
      ...encontrados, foto: actuales.foto, apariencia: actuales.apariencia, mascota: actuales.mascota,
    }));
    setErrorGeneral(Object.keys(encontrados).length ? t('editarPerfil.noGuardado') : '');
    if (Object.keys(encontrados).length || !hayCambios) return;

    setGuardando(true);
    try {
      let actualizada = cuenta;
      if (datosCambiaron) {
        actualizada = await editarDatos({ nombre: limpiar(formulario.nombre), apellido: limpiar(formulario.apellido) });
      }
      if (fotoPendiente?.tipo === 'nueva') actualizada = await cambiarFoto(fotoPendiente.foto);
      if (fotoPendiente?.tipo === 'quitar') actualizada = await quitarFoto();
      if (aparienciaCambio) actualizada = await cambiarApariencia(haciaServidor(apariencia));
      if (mascotaCambio) actualizada = await cambiarMascota(mascota);
      if (perfilPublicoCambio) {
        const nuevo = await editarPerfilPublico(haciaServidorPerfil(perfilPublico));
        setPerfil(nuevo);
        setPerfilPublico(publicoDesde(nuevo));
      }
      aplicarApariencia(desdeServidor(actualizada.apariencia), true);
      setFotoPendiente(null);
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
      setErrorGeneral(general || t('editarPerfil.noGuardado'));
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
          {t('editarPerfil.volver')}
        </Link>
        <div>
          <h1 className="text-[26px] font-extrabold sm:text-3xl">{t('editarPerfil.titulo')}</h1>
          <p className="mt-1 text-[15px] text-gray-600 sm:text-base">{t('editarPerfil.subtitulo')}</p>
        </div>

        {carga.fase === 'cargando' && (
          <div className="flex flex-col gap-5" aria-busy="true" aria-label={t('editarPerfil.cargando')}>
            <div className="h-40 animate-pulse rounded-2xl bg-white" />
            <div className="h-64 animate-pulse rounded-2xl bg-white" />
          </div>
        )}

        {carga.fase === 'error' && (
          <div role="alert" className="flex flex-col gap-4 rounded-2xl bg-red-50 p-5 text-red-700 sm:flex-row sm:items-center sm:p-6">
            <CircleAlert className="size-6 shrink-0" aria-hidden="true" />
            <div className="flex flex-1 flex-col gap-1">
              <p className="font-bold">{t('editarPerfil.errorCargar')}</p>
              <p>{carga.mensaje || t('editarPerfil.revisaConexion')}</p>
            </div>
            <button
              type="button"
              onClick={() => { setCarga({ fase: 'cargando' }); setIntento((n) => n + 1); }}
              className="h-11 rounded-xl bg-white px-5 font-bold text-red-700 hover:bg-red-100"
            >
              {t('editarPerfil.reintentar')}
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

            <section aria-labelledby="titulo-foto-perfil" className="flex flex-col items-center gap-4 rounded-2xl border border-zinc-200 bg-white p-5 text-center sm:flex-row sm:gap-6 sm:p-7 sm:text-left">
              {fotoMostrada ? (
                <img src={fotoMostrada} alt={t('editarPerfil.tuFoto')} className="size-28 shrink-0 rounded-full object-cover" />
              ) : (
                <AvatarUsuario iniciales={inicialesDe(formulario.nombre, formulario.apellido) || inicialesDe(cuenta.nombre, cuenta.apellido)} clase="size-28 text-4xl" />
              )}
              <div className="flex w-full flex-col gap-2.5">
                <h2 id="titulo-foto-perfil" className="text-lg font-extrabold">{t('editarPerfil.foto')}</h2>
                {errores.foto ? (
                  <MensajeError id="error-foto" texto={errores.foto} />
                ) : (
                  <p className="text-sm text-gray-600">
                    {fotoPendiente?.tipo === 'nueva' && t('editarPerfil.fotoNueva')}
                    {fotoPendiente?.tipo === 'quitar' && t('editarPerfil.fotoQuitar')}
                    {!fotoMostrada && !fotoPendiente && t('editarPerfil.sinFoto')}
                    {t('editarPerfil.formatos', { max: FOTO_MAX_MB })}
                  </p>
                )}
                <input
                  ref={selectorArchivo}
                  type="file"
                  accept={TIPOS_FOTO.join(',')}
                  onChange={alElegirArchivo}
                  className="hidden"
                  tabIndex={-1}
                  aria-hidden="true"
                />
                <div className="flex flex-col gap-2.5 sm:flex-row sm:flex-wrap">
                  <button
                    type="button"
                    onClick={elegirArchivo}
                    aria-describedby={errores.foto ? 'error-foto' : undefined}
                    className={`flex h-11 items-center justify-center gap-2 rounded-xl px-4 font-bold ${
                      fotoMostrada
                        ? 'border border-violet-300 bg-violet-50 text-violet-700 hover:bg-violet-100'
                        : 'bg-violet-600 text-white hover:bg-violet-700'
                    }`}
                  >
                    <Camera className="size-[18px]" aria-hidden="true" />
                    {fotoMostrada ? t('editarPerfil.cambiarFoto') : errores.foto ? t('editarPerfil.elegirOtra') : t('editarPerfil.subirFoto')}
                  </button>
                  {fotoMostrada && (
                    <button
                      type="button"
                      onClick={quitarLaFoto}
                      className="flex h-11 items-center justify-center gap-2 rounded-xl border border-zinc-200 bg-white px-4 font-bold text-red-700 hover:bg-red-50"
                    >
                      <Trash2 className="size-[18px]" aria-hidden="true" />
                      {t('editarPerfil.quitarFoto')}
                    </button>
                  )}
                </div>
              </div>
            </section>

            <section aria-labelledby="titulo-datos" className="flex flex-col gap-4 rounded-2xl border border-zinc-200 bg-white p-5 sm:p-7">
              <h2 id="titulo-datos" className="text-lg font-extrabold">{t('editarPerfil.tusDatos')}</h2>
              <div className="grid gap-4 sm:grid-cols-2">
                <CampoPerfil
                  id="nombre"
                  etiqueta={t('editarPerfil.nombre')}
                  valor={formulario.nombre}
                  onChange={cambiar('nombre')}
                  error={errores.nombre}
                  autoComplete="given-name"
                />
                <CampoPerfil
                  id="apellido"
                  etiqueta={t('editarPerfil.apellido')}
                  valor={formulario.apellido}
                  onChange={cambiar('apellido')}
                  error={errores.apellido}
                  autoComplete="family-name"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label htmlFor="correo" className="text-sm font-bold text-gray-700">{t('editarPerfil.correo')}</label>
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
                <p id="ayuda-correo" className="text-[13px] text-gray-600">{t('editarPerfil.correoAyuda')}</p>
              </div>
            </section>

            <SeccionPerfilPublico
              valor={perfilPublico}
              onCambiar={(nuevo) => {
                setPerfilPublico(nuevo);
                setErrores((actuales) => ({
                  ...actuales, frase: undefined, portada: undefined, insigniaDestacada: undefined, metasDestacadas: undefined,
                }));
                setGuardado(false);
              }}
              errores={{
                frase: errores.frase,
                portada: errores.portada,
                insigniaDestacada: errores.insigniaDestacada,
                metasDestacadas: errores.metasDestacadas,
              }}
              insignias={perfil?.insignias ?? []}
              metas={metasVisibles}
              vistaPrevia={{
                nombre: limpiar(formulario.nombre) || cuenta.nombre,
                apellido: limpiar(formulario.apellido) || cuenta.apellido,
                foto: fotoMostrada,
                fechaRegistro: perfil?.fechaRegistro,
                mascota,
              }}
            />

            <SeccionMascota
              mascota={mascota}
              onCambiar={(nueva) => {
                setMascota(nueva);
                setErrores((actuales) => ({ ...actuales, mascota: undefined }));
                setGuardado(false);
              }}
              error={errores.mascota}
            />

            <SeccionApariencia apariencia={apariencia} onCambiar={elegirApariencia} error={errores.apariencia} />

            <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <Link
                to="/metas"
                className="flex h-12 items-center justify-center rounded-xl border border-zinc-200 bg-white px-6 font-bold text-gray-700 hover:bg-gray-50"
              >
                {t('editarPerfil.cancelar')}
              </Link>
              <button
                type="submit"
                disabled={guardando || !hayCambios}
                className="flex h-12 items-center justify-center gap-2 rounded-xl bg-violet-600 px-6 font-bold text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50"
              >
                {guardando && <LoaderCircle className="size-5 animate-spin" aria-hidden="true" />}
                {guardando ? t('editarPerfil.guardando') : t('editarPerfil.guardar')}
              </button>
            </div>
          </form>
        )}
      </main>

      {archivoElegido && (
        <FotoPerfilModal
          archivo={archivoElegido}
          onUsar={usarFoto}
          onElegirOtra={() => { setArchivoElegido(null); elegirArchivo(); }}
          onCancelar={() => setArchivoElegido(null)}
          onError={alFallarFoto}
        />
      )}

      {guardado && (
        <p
          role="status"
          className="fixed bottom-6 left-1/2 z-40 flex w-[calc(100%-2rem)] max-w-md -translate-x-1/2 items-center gap-2.5 rounded-xl bg-emerald-400 px-5 py-3.5 text-[15px] font-bold text-emerald-950 shadow-lg"
        >
          <CircleCheck className="size-6 shrink-0" aria-hidden="true" />
          {t('editarPerfil.actualizado')}
        </p>
      )}
    </div>
  );
}
