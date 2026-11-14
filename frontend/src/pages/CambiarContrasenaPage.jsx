import { useEffect, useState } from 'react';
import { ArrowLeft, CircleAlert, CircleCheck, KeyRound, LoaderCircle, ShieldCheck } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Encabezado from '../components/Encabezado';
import CampoTexto from '../components/CampoTexto';
import RequisitosContrasena from '../components/RequisitosContrasena';
import { cambiarContrasena } from '../services/cuentaService';
import { actualizarSesion } from '../services/sesionService';
import { validarContrasenaNueva } from '../utils/contrasena';

const VACIO = { nueva: '', confirmacion: '' };

/**
 * HU32: cambiar la contraseña dentro de la app. Como ya hay sesión, solo pide la nueva (con sus
 * requisitos a la vista) y la confirmación. Al cambiarla, el servidor cierra las demás sesiones y
 * esta sigue con un token nuevo.
 */
export default function CambiarContrasenaPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [formulario, setFormulario] = useState(VACIO);
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [listo, setListo] = useState(false);

  // Se llega desde abajo de Editar perfil: la pantalla abre desde arriba.
  useEffect(() => { window.scrollTo(0, 0); }, []);

  const cambiar = (campo) => (valor) => {
    setFormulario((actual) => ({ ...actual, [campo]: valor }));
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }));
    setErrorGeneral('');
  };

  const enviar = async (evento) => {
    evento.preventDefault();
    const encontrados = validarContrasenaNueva(formulario, t);
    setErrores(encontrados);
    if (Object.keys(encontrados).length) {
      setErrorGeneral(t('contrasena.noCambiada'));
      return;
    }
    setEnviando(true);
    setErrorGeneral('');
    try {
      const { token } = await cambiarContrasena(formulario);
      actualizarSesion({ token });
      setFormulario(VACIO);
      setListo(true);
      window.scrollTo(0, 0);
    } catch (error) {
      if (error.estado === 401) {
        navigate('/login', { replace: true });
        return;
      }
      // Con la regla y la confirmación ya revisadas aquí, un 400 es que la nueva es igual a la actual.
      if (error.estado === 400 && error.errores?.length) {
        setErrores({ nueva: error.errores.join(' ') });
        setErrorGeneral(t('contrasena.noCambiada'));
      } else {
        setErrorGeneral(error.message);
      }
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />
      <main className="mx-auto flex max-w-[560px] flex-col gap-5 px-4 pb-12 pt-6 sm:pt-10">
        <Link to="/perfil/editar" className="inline-flex items-center gap-1 self-start text-[15px] font-bold text-violet-700 hover:underline">
          <ArrowLeft className="size-[18px]" aria-hidden="true" />
          {t('contrasena.volver')}
        </Link>
        <div>
          <h1 className="text-[26px] font-extrabold sm:text-3xl">{t('contrasena.titulo')}</h1>
          <p className="mt-1 text-[15px] text-gray-600 sm:text-base">{t('contrasena.subtitulo')}</p>
        </div>

        {listo ? (
          <section role="status" className="flex flex-col items-center gap-4 rounded-2xl border border-zinc-200 bg-white p-6 text-center sm:p-8">
            <div className="flex size-16 items-center justify-center rounded-full bg-emerald-50 text-emerald-600">
              <CircleCheck className="size-9" aria-hidden="true" />
            </div>
            <h2 className="text-xl font-extrabold">{t('contrasena.listoTitulo')}</h2>
            <p className="max-w-sm text-[15px] leading-6 text-gray-600">{t('contrasena.listoTexto')}</p>
            <div className="flex w-full flex-col gap-3 pt-2 sm:w-auto sm:flex-row">
              <Link to="/perfil/editar" className="flex h-12 items-center justify-center rounded-xl border border-zinc-200 bg-white px-6 font-bold text-gray-700 hover:bg-gray-50">
                {t('contrasena.irEditar')}
              </Link>
              <Link to="/metas" className="flex h-12 items-center justify-center rounded-xl bg-violet-600 px-6 font-bold text-white hover:bg-violet-700">
                {t('contrasena.irMetas')}
              </Link>
            </div>
          </section>
        ) : (
          <form onSubmit={enviar} noValidate className="flex flex-col gap-5 rounded-2xl border border-zinc-200 bg-white p-5 sm:p-7">
            {errorGeneral && (
              <p role="alert" className="flex items-start gap-2.5 rounded-xl border border-red-200 bg-red-50 px-4 py-3.5 text-[15px] text-red-800">
                <CircleAlert className="mt-0.5 size-5 shrink-0" aria-hidden="true" />
                <span>{errorGeneral}</span>
              </p>
            )}


            <div className="flex flex-col gap-2.5">
              <CampoTexto
                id="contrasena-nueva"
                etiqueta={t('contrasena.nueva')}
                tipo="password"
                valor={formulario.nueva}
                onChange={cambiar('nueva')}
                error={errores.nueva}
                deshabilitado={enviando}
                autoComplete="new-password"
              />
              <RequisitosContrasena valor={formulario.nueva} />
            </div>

            <CampoTexto
              id="contrasena-confirmacion"
              etiqueta={t('contrasena.confirmar')}
              tipo="password"
              valor={formulario.confirmacion}
              onChange={cambiar('confirmacion')}
              error={errores.confirmacion}
              deshabilitado={enviando}
              autoComplete="new-password"
            />

            <p className="flex items-start gap-2.5 rounded-xl bg-violet-50 px-4 py-3 text-sm leading-5 text-violet-900">
              <ShieldCheck className="mt-0.5 size-[18px] shrink-0" aria-hidden="true" />
              {t('contrasena.aviso')}
            </p>

            <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <Link
                to="/perfil/editar"
                className="flex h-12 items-center justify-center rounded-xl border border-zinc-200 bg-white px-6 font-bold text-gray-700 hover:bg-gray-50"
              >
                {t('contrasena.cancelar')}
              </Link>
              <button
                type="submit"
                disabled={enviando}
                className="flex h-12 items-center justify-center gap-2 rounded-xl bg-violet-600 px-6 font-bold text-white hover:bg-violet-700 disabled:opacity-60"
              >
                {enviando
                  ? <LoaderCircle className="size-[18px] animate-spin" aria-hidden="true" />
                  : <KeyRound className="size-[18px]" aria-hidden="true" />}
                {enviando ? t('contrasena.cambiando') : t('contrasena.guardar')}
              </button>
            </div>
          </form>
        )}
      </main>
    </div>
  );
}
