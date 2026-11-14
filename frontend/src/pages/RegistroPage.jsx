import { useEffect, useState } from 'react';
import { LoaderCircle, MailCheck } from 'lucide-react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import i18n from '../i18n';
import LayoutAuth from '../components/LayoutAuth';
import CampoTexto from '../components/CampoTexto';
import AlertaFormulario from '../components/AlertaFormulario';
import EnlaceNoValido from '../components/EnlaceNoValido';
import { consultarSolicitud, registrar } from '../services/authService';
import { TEXTO_USUARIO_MAX } from '../constants/validaciones';

const FORMULARIO_VACIO = { nombre: '', apellido: '', password: '' };

function validar({ nombre, apellido, password }) {
  const errores = {};
  if (!nombre.trim()) errores.nombre = i18n.t('auth.validacion.nombreObligatorio');
  if (!apellido.trim()) errores.apellido = i18n.t('auth.validacion.apellidoObligatorio');
  if (password.length < 8) errores.password = i18n.t('auth.validacion.passwordMinimo');
  else if (!/[a-z]/.test(password) || !/[A-Z]/.test(password) || !/\d/.test(password)) {
    errores.password = i18n.t('auth.validacion.passwordFormato');
  }
  return errores;
}

/**
 * HU8 paso 3: llega desde el enlace del correo (/registro/completar?token=...).
 * Solo si el enlace es válido se muestra el formulario, con el email ya autorizado.
 */
export default function RegistroPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [parametros] = useSearchParams();
  const token = parametros.get('token') ?? '';
  // { fase: 'cargando' | 'invalido' | 'lista', email?, mensaje? }
  const [solicitud, setSolicitud] = useState(() => (token ? { fase: 'cargando' } : { fase: 'invalido' }));
  const [formulario, setFormulario] = useState(FORMULARIO_VACIO);
  const [errores, setErrores] = useState({});
  const [errorServidor, setErrorServidor] = useState('');
  const [enviando, setEnviando] = useState(false);

  // Comprobar el enlace antes de mostrar el formulario.
  useEffect(() => {
    if (!token) return undefined;
    let activo = true;
    consultarSolicitud(token)
      .then((datos) => activo && setSolicitud({ fase: 'lista', email: datos.email }))
      .catch((e) => activo && setSolicitud({
        fase: 'invalido',
        mensaje: e.estado === 410 ? e.message : i18n.t('auth.registro.errorComprobar'),
      }));
    return () => {
      activo = false;
    };
  }, [token]);

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
      await registrar({
        token,
        nombre: formulario.nombre.trim(),
        apellido: formulario.apellido.trim(),
        password: formulario.password,
      });
      navigate('/login', { state: { cuentaCreada: true } });
    } catch (error) {
      if (error.estado === 410) {
        setSolicitud({ fase: 'invalido', mensaje: error.message });
      } else if (error.estado === 409) {
        setErrorServidor(t('auth.registro.yaRegistrado'));
      } else {
        const detalle = error.errores?.length ? `: ${error.errores.join(', ')}` : '';
        setErrorServidor(`${error.message}${detalle}`);
      }
    } finally {
      setEnviando(false);
    }
  };

  if (solicitud.fase === 'cargando') {
    return (
      <LayoutAuth titulo={t('auth.registro.titulo')} subtitulo={t('auth.registro.comprobandoSubtitulo')}>
        <p role="status" className="flex items-center justify-center gap-2 py-8 text-sm text-gray-600">
          <LoaderCircle className="size-5 animate-spin text-violet-600" aria-hidden="true" />
          {t('auth.registro.comprobando')}
        </p>
      </LayoutAuth>
    );
  }

  if (solicitud.fase === 'invalido') {
    return (
      <LayoutAuth titulo={t('auth.registro.invalidoTitulo')} subtitulo={t('auth.registro.invalidoSubtitulo')}>
        <EnlaceNoValido mensaje={solicitud.mensaje} />
      </LayoutAuth>
    );
  }

  return (
    <LayoutAuth titulo={t('auth.registro.completaTitulo')} subtitulo={t('auth.registro.completaSubtitulo')}>
      <AlertaFormulario texto={errorServidor} />

      <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <CampoTexto
            id="nombre"
            etiqueta={t('auth.campos.nombre')}
            valor={formulario.nombre}
            onChange={(v) => actualizar('nombre', v)}
            placeholder={t('auth.campos.nombrePlaceholder')}
            error={errores.nombre}
            deshabilitado={enviando}
            autoComplete="given-name"
            maxLength={TEXTO_USUARIO_MAX}
          />
          <CampoTexto
            id="apellido"
            etiqueta={t('auth.campos.apellido')}
            valor={formulario.apellido}
            onChange={(v) => actualizar('apellido', v)}
            placeholder={t('auth.campos.apellidoPlaceholder')}
            error={errores.apellido}
            deshabilitado={enviando}
            autoComplete="family-name"
            maxLength={TEXTO_USUARIO_MAX}
          />
        </div>

        <div className="flex flex-col gap-1.5">
          <span className="text-sm font-bold text-gray-900">{t('auth.campos.email')}</span>
          <p className="flex h-[46px] items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-4 text-[15px] text-gray-900">
            <MailCheck className="size-[18px] shrink-0 text-emerald-700" aria-hidden="true" />
            <span className="truncate">{solicitud.email}</span>
            <span className="ml-auto shrink-0 text-xs font-bold text-emerald-700">{t('auth.registro.autorizado')}</span>
          </p>
        </div>

        <CampoTexto
          id="password"
          etiqueta={t('auth.campos.password')}
          tipo="password"
          valor={formulario.password}
          onChange={(v) => actualizar('password', v)}
          placeholder={t('auth.registro.passwordPlaceholder')}
          error={errores.password}
          deshabilitado={enviando}
          autoComplete="new-password"
        />

        <button
          type="submit"
          disabled={enviando}
          className="flex h-[46px] items-center justify-center gap-2 rounded-xl bg-violet-600 text-[15px] font-bold text-white transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:bg-violet-400"
        >
          {enviando ? (
            <>
              <LoaderCircle className="size-[18px] animate-spin" aria-hidden="true" />
              {t('auth.registro.enviando')}
            </>
          ) : (
            t('auth.registro.boton')
          )}
        </button>

        <p className="text-center text-sm text-gray-500">
          {t('auth.solicitar.yaTienesCuenta')}{' '}
          <Link to="/login" className="font-bold text-violet-600 hover:underline">{t('auth.solicitar.iniciaSesion')}</Link>
        </p>
      </form>
    </LayoutAuth>
  );
}
