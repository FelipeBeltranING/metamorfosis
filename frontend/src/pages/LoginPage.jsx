import { useEffect, useState } from 'react';
import { CircleCheck, LoaderCircle } from 'lucide-react';
import {
  Link, useLocation, useNavigate, useSearchParams,
} from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import i18n from '../i18n';
import LayoutAuth from '../components/LayoutAuth';
import CampoTexto from '../components/CampoTexto';
import AlertaFormulario from '../components/AlertaFormulario';
import { iniciarSesion } from '../services/authService';
import { guardarSesion } from '../services/sesionService';
import { REGEX_EMAIL, TEXTO_USUARIO_MAX } from '../constants/validaciones';

/** Qué aviso mostrar al llegar (viene en el estado de navegación). */
function avisoInicial(estadoNavegacion) {
  if (estadoNavegacion?.cuentaCreada) return 'cuentaCreada';
  if (estadoNavegacion?.sesionCerrada) return 'sesionCerrada';
  return null;
}

/** Validación del cliente. Las claves coinciden con los id de los campos. */
function validar({ email, password }) {
  const errores = {};
  if (!REGEX_EMAIL.test(email.trim())) errores.email = i18n.t('auth.validacion.emailInvalido');
  if (!password) errores.password = i18n.t('auth.validacion.passwordObligatoria');
  return errores;
}

export default function LoginPage() {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const [parametros] = useSearchParams();
  const [formulario, setFormulario] = useState({ email: '', password: '' });
  const [errores, setErrores] = useState({});
  const [errorServidor, setErrorServidor] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [aviso, setAviso] = useState(() => avisoInicial(location.state));

  // Aviso de éxito: se limpia el estado de navegación y se oculta a los 4 s.
  useEffect(() => {
    if (!aviso) return undefined;
    navigate(location.pathname + location.search, { replace: true, state: null });
    const temporizador = setTimeout(() => setAviso(null), 4000);
    return () => clearTimeout(temporizador);
  }, [aviso, navigate, location.pathname, location.search]);

  const actualizar = (campo, valor) => {
    setFormulario((anterior) => ({ ...anterior, [campo]: valor }));
    setErrores((anterior) => ({ ...anterior, [campo]: undefined }));
    setErrorServidor('');
  };

  const enviar = async (evento) => {
    evento.preventDefault();
    setErrorServidor('');

    const erroresCliente = validar(formulario);
    setErrores(erroresCliente);
    const primerCampoInvalido = Object.keys(erroresCliente)[0];
    if (primerCampoInvalido) {
      document.getElementById(primerCampoInvalido)?.focus();
      return;
    }

    setEnviando(true);
    try {
      const sesion = await iniciarSesion({
        email: formulario.email.trim(),
        password: formulario.password,
      });
      guardarSesion(sesion);
      // HU14: si se llegó desde un enlace (p. ej. aceptar una invitación), se vuelve ahí.
      const volver = parametros.get('volver');
      if (volver && volver.startsWith('/') && !volver.startsWith('//')) navigate(volver, { replace: true });
      else navigate('/metas', { replace: true, state: { sesionIniciada: true } });
    } catch (error) {
      if (error.estado === 401) {
        setErrorServidor(t('auth.login.credenciales'));
      } else if (error.estado === 429) {
        // HU9: demasiados intentos fallidos; el servidor dice cuántos minutos esperar.
        setErrorServidor(error.message);
      } else {
        const detalle = error.errores?.length ? `: ${error.errores.join(', ')}` : '';
        setErrorServidor(`${error.message}${detalle}`);
      }
      setEnviando(false);
    }
  };

  return (
    <LayoutAuth titulo={t('auth.login.titulo')} subtitulo={t('auth.login.subtitulo')}>
      <AlertaFormulario texto={errorServidor} />

      <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
        <CampoTexto
          id="email"
          etiqueta={t('auth.campos.email')}
          tipo="email"
          valor={formulario.email}
          onChange={(v) => actualizar('email', v)}
          placeholder={t('auth.campos.emailPlaceholder')}
          error={errores.email}
          deshabilitado={enviando}
          autoComplete="email"
          maxLength={TEXTO_USUARIO_MAX}
        />

        <CampoTexto
          id="password"
          etiqueta={t('auth.campos.password')}
          tipo="password"
          valor={formulario.password}
          onChange={(v) => actualizar('password', v)}
          placeholder={t('auth.login.passwordPlaceholder')}
          error={errores.password}
          deshabilitado={enviando}
          autoComplete="current-password"
        />

        <button
          type="submit"
          disabled={enviando}
          className="flex h-[46px] items-center justify-center gap-2 rounded-xl bg-violet-600 text-[15px] font-bold text-white transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:bg-violet-400"
        >
          {enviando ? (
            <>
              <LoaderCircle className="size-[18px] animate-spin" aria-hidden="true" />
              {t('auth.login.enviando')}
            </>
          ) : (
            t('auth.login.boton')
          )}
        </button>

        <p className="text-center text-sm text-gray-500">
          {t('auth.login.sinCuenta')}{' '}
          <Link to="/registro" className="font-bold text-violet-600 hover:underline">{t('auth.login.registrate')}</Link>
        </p>
      </form>

      {aviso && (
        <div
          role="status"
          className="fixed bottom-10 left-1/2 flex w-[calc(100%-2rem)] max-w-[440px] -translate-x-1/2 items-center gap-3 rounded-xl bg-emerald-400 px-4 py-3 text-sm font-bold text-gray-900 shadow-lg"
        >
          <CircleCheck className="size-[18px] shrink-0" aria-hidden="true" />
          {t(`auth.login.${aviso}`)}
        </div>
      )}
    </LayoutAuth>
  );
}
