import { useEffect, useState } from 'react';
import { ArrowLeft, KeyRound, LoaderCircle, MailCheck } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import LayoutAuth from '../components/LayoutAuth';
import CampoTexto from '../components/CampoTexto';
import AlertaFormulario from '../components/AlertaFormulario';
import MensajeError from '../components/MensajeError';
import RequisitosContrasena from '../components/RequisitosContrasena';
import { restablecerContrasena, solicitarCodigoContrasena } from '../services/authService';
import { validarContrasenaNueva } from '../utils/contrasena';
import { REGEX_EMAIL, TEXTO_USUARIO_MAX } from '../constants/validaciones';

/** Segundos antes de poder pedir otro código (el servidor no manda más de uno por minuto). */
const ESPERA_REENVIO = 60;

/** «482913» → «482 913» mientras se escribe; solo dígitos, máximo 6. */
function formatearCodigo(texto) {
  const digitos = texto.replace(/\D/g, '').slice(0, 6);
  return digitos.length > 3 ? `${digitos.slice(0, 3)} ${digitos.slice(3)}` : digitos;
}

/**
 * HU33: olvidé mi contraseña. Paso 1: el correo, al que llega un código de 6 dígitos. Paso 2: el
 * código y la contraseña nueva dos veces. Al terminar vuelve a Iniciar sesión con el correo puesto.
 */
export default function RecuperarContrasenaPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const [paso, setPaso] = useState('correo');
  const [email, setEmail] = useState(location.state?.email ?? '');
  const [formulario, setFormulario] = useState({ codigo: '', nueva: '', confirmacion: '' });
  const [errores, setErrores] = useState({});
  const [errorServidor, setErrorServidor] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [espera, setEspera] = useState(0);
  const [reenviado, setReenviado] = useState(false);

  useEffect(() => {
    if (espera <= 0) return undefined;
    const temporizador = setTimeout(() => setEspera((s) => s - 1), 1000);
    return () => clearTimeout(temporizador);
  }, [espera]);

  const pedirCodigo = async ({ reenvio = false } = {}) => {
    setErrorServidor('');
    if (!REGEX_EMAIL.test(email.trim())) {
      setErrores({ email: t('auth.validacion.emailInvalido') });
      document.getElementById('email')?.focus();
      return;
    }
    setEnviando(true);
    try {
      await solicitarCodigoContrasena(email.trim());
      setPaso('codigo');
      setEspera(ESPERA_REENVIO);
      setReenviado(reenvio);
    } catch (error) {
      const detalle = error.errores?.length ? `: ${error.errores.join(', ')}` : '';
      setErrorServidor(`${error.message}${detalle}`);
    } finally {
      setEnviando(false);
    }
  };

  const cambiar = (campo) => (valor) => {
    setFormulario((actual) => ({ ...actual, [campo]: campo === 'codigo' ? formatearCodigo(valor) : valor }));
    setErrores((actuales) => ({ ...actuales, [campo]: undefined }));
    setErrorServidor('');
  };

  const cambiarContrasena = async (evento) => {
    evento.preventDefault();
    const encontrados = validarContrasenaNueva(formulario, t);
    if (formulario.codigo.replace(/\D/g, '').length !== 6) encontrados.codigo = t('recuperar.faltaCodigo');
    setErrores(encontrados);
    const primero = ['codigo', 'nueva', 'confirmacion'].find((campo) => encontrados[campo]);
    if (primero) {
      document.getElementById(`recuperar-${primero}`)?.focus();
      return;
    }
    setEnviando(true);
    setErrorServidor('');
    try {
      await restablecerContrasena({ email: email.trim(), ...formulario });
      navigate('/login', { replace: true, state: { contrasenaCambiada: true, email: email.trim() } });
    } catch (error) {
      // Con la contraseña nueva ya revisada aquí, un 400 es casi siempre el código; si no, la nueva.
      if (error.estado === 400 && error.errores?.length) {
        const texto = error.errores.join(' ');
        const esDeLaNueva = error.errores.some((e) => /contraseña|password/i.test(e) && !/código|code/i.test(e));
        setErrores(esDeLaNueva ? { nueva: texto } : { codigo: texto });
      } else {
        setErrorServidor(error.message);
      }
      setEnviando(false);
    }
  };

  if (paso === 'correo') {
    return (
      <LayoutAuth titulo={t('recuperar.titulo')} subtitulo={t('recuperar.subtitulo')}>
        <AlertaFormulario texto={errorServidor} />
        <form onSubmit={(e) => { e.preventDefault(); pedirCodigo(); }} noValidate className="flex flex-col gap-4">
          <CampoTexto
            id="email"
            etiqueta={t('auth.campos.email')}
            tipo="email"
            valor={email}
            onChange={(v) => { setEmail(v); setErrores({}); setErrorServidor(''); }}
            placeholder={t('auth.campos.emailPlaceholder')}
            error={errores.email}
            deshabilitado={enviando}
            autoComplete="email"
            maxLength={TEXTO_USUARIO_MAX}
          />
          <button
            type="submit"
            disabled={enviando}
            className="flex h-[46px] items-center justify-center gap-2 rounded-xl bg-violet-600 text-[15px] font-bold text-white transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:bg-violet-400"
          >
            {enviando && <LoaderCircle className="size-[18px] animate-spin" aria-hidden="true" />}
            {enviando ? t('recuperar.enviando') : t('recuperar.enviarCodigo')}
          </button>
          <Link to="/login" className="inline-flex items-center justify-center gap-1 text-sm font-bold text-violet-600 hover:underline">
            <ArrowLeft className="size-4" aria-hidden="true" />
            {t('recuperar.volverLogin')}
          </Link>
        </form>
      </LayoutAuth>
    );
  }

  return (
    <LayoutAuth titulo={t('recuperar.tituloCodigo')} subtitulo={t('recuperar.subtituloCodigo')}>
      <p role="status" className="mb-4 flex items-start gap-2.5 rounded-xl bg-violet-50 px-3.5 py-3 text-sm leading-5 text-violet-900">
        <MailCheck className="mt-0.5 size-[18px] shrink-0" aria-hidden="true" />
        <span>
          {reenviado ? t('recuperar.reenviado') : t('recuperar.enviado')} <strong>{email.trim()}</strong>.{' '}
          {t('recuperar.revisaSpam')}
        </span>
      </p>
      <AlertaFormulario texto={errorServidor} />
      <form onSubmit={cambiarContrasena} noValidate className="flex flex-col gap-4">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="recuperar-codigo" className="text-sm font-bold text-gray-900">{t('recuperar.codigo')} *</label>
          <input
            id="recuperar-codigo"
            value={formulario.codigo}
            onChange={(e) => cambiar('codigo')(e.target.value)}
            inputMode="numeric"
            autoComplete="one-time-code"
            placeholder="000 000"
            disabled={enviando}
            aria-required="true"
            aria-invalid={Boolean(errores.codigo)}
            aria-describedby={errores.codigo ? 'error-recuperar-codigo' : undefined}
            className={`h-14 w-full rounded-xl border px-4 text-center text-2xl font-extrabold tracking-[0.3em] text-gray-900 outline-none placeholder:text-gray-300 focus:ring-2 focus:ring-violet-300 ${errores.codigo ? 'border-red-400' : 'border-zinc-200'}`}
          />
          <MensajeError id="error-recuperar-codigo" texto={errores.codigo} />
        </div>

        <div className="flex flex-col gap-2.5">
          <CampoTexto
            id="recuperar-nueva"
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
          id="recuperar-confirmacion"
          etiqueta={t('contrasena.confirmar')}
          tipo="password"
          valor={formulario.confirmacion}
          onChange={cambiar('confirmacion')}
          error={errores.confirmacion}
          deshabilitado={enviando}
          autoComplete="new-password"
        />

        <button
          type="submit"
          disabled={enviando}
          className="flex h-[46px] items-center justify-center gap-2 rounded-xl bg-violet-600 text-[15px] font-bold text-white transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:bg-violet-400"
        >
          {enviando
            ? <LoaderCircle className="size-[18px] animate-spin" aria-hidden="true" />
            : <KeyRound className="size-[18px]" aria-hidden="true" />}
          {enviando ? t('contrasena.cambiando') : t('contrasena.guardar')}
        </button>

        <div className="flex flex-col items-center gap-2 text-sm">
          <button
            type="button"
            onClick={() => pedirCodigo({ reenvio: true })}
            disabled={enviando || espera > 0}
            className="font-bold text-violet-600 hover:underline disabled:cursor-not-allowed disabled:text-gray-400 disabled:no-underline"
          >
            {espera > 0 ? t('recuperar.reenviarEn', { segundos: espera }) : t('recuperar.reenviar')}
          </button>
          <button
            type="button"
            onClick={() => { setPaso('correo'); setErrores({}); setErrorServidor(''); setFormulario({ codigo: '', nueva: '', confirmacion: '' }); }}
            className="font-semibold text-gray-600 hover:underline"
          >
            {t('recuperar.otroCorreo')}
          </button>
        </div>
      </form>
    </LayoutAuth>
  );
}
