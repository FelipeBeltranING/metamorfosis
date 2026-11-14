import { useEffect, useState } from 'react';
import {
  CircleAlert, CircleCheck, Link2Off, LoaderCircle, UserPlus,
} from 'lucide-react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import LayoutAuth from '../components/LayoutAuth';
import {
  aceptarInvitacionCorreo, consultarInvitacionCorreo, rechazarInvitacionCorreo,
} from '../services/invitacionService';
import { obtenerSesion } from '../services/sesionService';

const BOTON = 'flex h-[46px] w-full items-center justify-center gap-2 rounded-xl font-bold disabled:opacity-60';

/**
 * HU14: página a la que llevan los botones del correo de invitación. Muestra la invitación y
 * pide confirmar con un clic (los lectores de correo abren los enlaces solos, así que nada se
 * responde al cargar). Aceptar exige haber iniciado sesión con la cuenta invitada; si no hay
 * sesión, se va al login y se vuelve aquí. Rechazar no pide sesión.
 */
export default function ResponderInvitacionPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const [params] = useSearchParams();
  const token = params.get('token') ?? '';
  const [accion, setAccion] = useState(params.get('accion') === 'rechazar' ? 'rechazar' : 'aceptar');
  const [carga, setCarga] = useState({ fase: 'cargando' });
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');
  const [rechazada, setRechazada] = useState(false);
  const conSesion = Boolean(obtenerSesion());

  useEffect(() => {
    let activo = true;
    consultarInvitacionCorreo(token)
      .then((invitacion) => activo && setCarga({ fase: 'lista', invitacion }))
      .catch((e) => activo && setCarga({ fase: 'error', mensaje: e.message }));
    return () => { activo = false; };
  }, [token]);

  const volverAqui = `/login?volver=${encodeURIComponent(location.pathname + location.search)}`;

  const aceptar = async () => {
    setEnviando(true);
    setError('');
    try {
      const r = await aceptarInvitacionCorreo(token);
      navigate(`/metas/${r.codigoMeta}`, { replace: true });
    } catch (e) {
      if (e.estado === 401) navigate(volverAqui, { replace: true });
      else setError(e.message);
      setEnviando(false);
    }
  };

  const rechazar = async () => {
    setEnviando(true);
    setError('');
    try {
      await rechazarInvitacionCorreo(token);
      setRechazada(true);
    } catch (e) {
      setError(e.message);
    } finally {
      setEnviando(false);
    }
  };

  if (carga.fase === 'cargando') {
    return (
      <LayoutAuth titulo={t('responder.titulo')} subtitulo={t('responder.unMomento')}>
        <div className="flex justify-center py-8"><LoaderCircle className="size-8 animate-spin text-violet-600" aria-label={t('responder.cargando')} /></div>
      </LayoutAuth>
    );
  }

  if (carga.fase === 'error') {
    return (
      <LayoutAuth titulo={t('responder.titulo')} subtitulo={t('responder.errorSubtitulo')}>
        <div className="flex flex-col items-center text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-red-50 text-red-600">
            <Link2Off className="size-8" aria-hidden="true" />
          </div>
          <p className="mt-4 text-[15px] leading-6 text-gray-600">{carga.mensaje}</p>
          <Link to={conSesion ? '/invitaciones' : '/login'} className={`${BOTON} mt-6 bg-violet-600 text-white hover:bg-violet-700`}>
            {conSesion ? t('responder.verInvitaciones') : t('responder.iniciarSesion')}
          </Link>
        </div>
      </LayoutAuth>
    );
  }

  const { invitacion } = carga;

  if (rechazada) {
    return (
      <LayoutAuth titulo={t('responder.rechazadaTitulo')} subtitulo={invitacion.nombreMeta}>
        <div className="flex flex-col items-center gap-4 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-slate-100 text-gray-600">
            <CircleCheck className="size-8" aria-hidden="true" />
          </div>
          <p className="text-[15px] leading-6 text-gray-600">
            {t('responder.rechazadaTexto', { nombre: invitacion.nombreInvitador })}
          </p>
          <Link to={conSesion ? '/metas' : '/login'} className="font-bold text-violet-700 hover:underline">
            {conSesion ? t('responder.irMetas') : t('responder.irApp')}
          </Link>
        </div>
      </LayoutAuth>
    );
  }

  return (
    <LayoutAuth titulo={t('responder.teInvito', { nombre: invitacion.nombreInvitador })} subtitulo={t('responder.para', { email: invitacion.paraEmail })}>
      <div className="flex flex-col gap-5">
        <div className="flex items-center gap-3 rounded-xl bg-violet-50 p-4">
          <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-white text-violet-600">
            <UserPlus className="size-6" aria-hidden="true" />
          </div>
          <p className="break-words text-lg font-extrabold text-violet-900">{invitacion.nombreMeta}</p>
        </div>

        {accion === 'aceptar' ? (
          <>
            <p className="text-[15px] leading-6 text-gray-600">
              {t('responder.siAceptas')}
            </p>
            {error && (
              <p role="alert" className="flex items-start gap-2 rounded-xl bg-red-50 p-3 text-sm font-semibold text-red-700">
                <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden="true" />{error}
              </p>
            )}
            {conSesion ? (
              <button type="button" onClick={aceptar} disabled={enviando} className={`${BOTON} bg-violet-600 text-white hover:bg-violet-700`}>
                {enviando && <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />}
                {t('responder.aceptar')}
              </button>
            ) : (
              <>
                <Link to={volverAqui} className={`${BOTON} bg-violet-600 text-white hover:bg-violet-700`}>
                  {t('responder.iniciaParaAceptar')}
                </Link>
                <p className="-mt-2 text-center text-sm text-gray-500">
                  {t('responder.sinCuenta')}{' '}
                  <Link to="/registro" className="font-bold text-violet-600 hover:underline">{t('responder.registrate')}</Link>
                  {' '}{t('responder.conEmail', { email: invitacion.paraEmail })}
                </p>
              </>
            )}
            <button type="button" onClick={() => { setAccion('rechazar'); setError(''); }} className="text-sm font-bold text-gray-600 hover:underline">
              {t('responder.prefieroRechazar')}
            </button>
          </>
        ) : (
          <>
            <p className="text-[15px] leading-6 text-gray-600">
              {t('responder.quieresRechazar', { nombre: invitacion.nombreInvitador })}
            </p>
            {error && <p role="alert" className="text-sm font-semibold text-red-600">{error}</p>}
            <button type="button" onClick={rechazar} disabled={enviando} className={`${BOTON} border border-zinc-300 bg-white text-gray-800 hover:bg-gray-50`}>
              {enviando && <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />}
              {t('responder.rechazar')}
            </button>
            <button type="button" onClick={() => { setAccion('aceptar'); setError(''); }} className="text-sm font-bold text-violet-700 hover:underline">
              {t('responder.mejorAcepto')}
            </button>
          </>
        )}
      </div>
    </LayoutAuth>
  );
}
