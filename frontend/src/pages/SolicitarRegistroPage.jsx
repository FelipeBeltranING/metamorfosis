import { useEffect, useState } from 'react';
import { CircleAlert, LoaderCircle, MailCheck } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import LayoutAuth from '../components/LayoutAuth';
import CampoTexto from '../components/CampoTexto';
import AlertaFormulario from '../components/AlertaFormulario';
import { consultarEstado, solicitarRegistro } from '../services/authService';
import { CANAL_REGISTRO, INTERVALO_ESTADO_MS } from '../constants/registro';
import { REGEX_EMAIL, TEXTO_USUARIO_MAX } from '../constants/validaciones';

const ESPERA_REENVIO = 60; // segundos, igual que el servidor

const MENSAJES_FIN = {
  RECHAZADA: 'Desde el correo se respondió "No fui yo", así que este registro se anuló.',
  VENCIDA: 'El enlace venció antes de que lo autorizaras.',
  INACTIVA: 'Esta solicitud ya no está activa.',
};

/**
 * HU8 paso 1: el usuario escribe su correo y recibe el enlace para autorizar el registro.
 * Esta página se queda esperando: cuando se autoriza desde el correo (en esta u otra
 * pestaña, o en el celular), abre el formulario sola.
 */
export default function SolicitarRegistroPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [errorServidor, setErrorServidor] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [enviadoA, setEnviadoA] = useState('');
  const [espera, setEspera] = useState(0);
  const [sesion, setSesion] = useState('');
  const [fin, setFin] = useState(''); // RECHAZADA | VENCIDA | INACTIVA

  // Cuenta regresiva para poder reenviar.
  useEffect(() => {
    if (espera <= 0) return undefined;
    const temporizador = setTimeout(() => setEspera((s) => s - 1), 1000);
    return () => clearTimeout(temporizador);
  }, [espera]);

  // Esperar la autorización: pregunta cada pocos segundos, al volver a la pestaña y cuando
  // otra pestaña de este navegador avisa que ya se autorizó.
  useEffect(() => {
    if (!sesion || fin) return undefined;
    let activo = true;

    const revisar = async () => {
      try {
        const { estado } = await consultarEstado(sesion);
        if (!activo) return;
        if (estado === 'AUTORIZADA') {
          navigate(`/registro/completar?token=${encodeURIComponent(sesion)}`, { replace: true });
        } else if (estado === 'RECHAZADA' || estado === 'VENCIDA') {
          setFin(estado);
        }
      } catch (e) {
        if (activo && e.estado === 410) setFin('INACTIVA');
        // Sin conexión: se vuelve a intentar en la siguiente vuelta.
      }
    };

    const intervalo = setInterval(revisar, INTERVALO_ESTADO_MS);
    const alVolver = () => {
      if (document.visibilityState === 'visible') revisar();
    };
    document.addEventListener('visibilitychange', alVolver);
    let canal = null;
    try {
      canal = new BroadcastChannel(CANAL_REGISTRO);
      canal.onmessage = revisar;
    } catch {
      // Sin BroadcastChannel: basta con preguntar cada pocos segundos.
    }

    return () => {
      activo = false;
      clearInterval(intervalo);
      document.removeEventListener('visibilitychange', alVolver);
      canal?.close();
    };
  }, [sesion, fin, navigate]);

  const empezarDeNuevo = () => {
    setEnviadoA('');
    setSesion('');
    setFin('');
    setErrorServidor('');
  };

  const pedir = async (correo) => {
    setErrorServidor('');
    setEnviando(true);
    try {
      const datos = await solicitarRegistro(correo);
      setEnviadoA(datos?.email ?? correo);
      setSesion(datos?.sesion ?? '');
      setFin('');
      setEspera(ESPERA_REENVIO);
    } catch (e) {
      if (e.estado === 409) {
        setError('Este correo ya está registrado');
        setErrorServidor('Este correo ya tiene una cuenta. Inicia sesión o usa otro correo.');
        setEnviadoA('');
        setSesion('');
      } else if (e.estado === 400 && e.errores?.length) {
        setError(e.errores[0]);
      } else {
        setErrorServidor(e.message);
      }
    } finally {
      setEnviando(false);
    }
  };

  const enviar = (evento) => {
    evento.preventDefault();
    const correo = email.trim();
    if (!REGEX_EMAIL.test(correo)) {
      setError('Ingresa un correo válido');
      document.getElementById('email')?.focus();
      return;
    }
    pedir(correo);
  };

  if (enviadoA && fin) {
    return (
      <LayoutAuth titulo="Registro anulado" subtitulo="No se creó ninguna cuenta.">
        <div className="flex flex-col items-center text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-amber-50 text-amber-700">
            <CircleAlert className="size-8" aria-hidden="true" />
          </div>
          <p className="mt-4 text-[15px] leading-6 text-gray-600">{MENSAJES_FIN[fin]}</p>
          <button
            type="button"
            onClick={empezarDeNuevo}
            className="mt-6 flex h-[46px] w-full items-center justify-center rounded-xl bg-violet-600 font-bold text-white hover:bg-violet-700"
          >
            Empezar de nuevo
          </button>
        </div>
      </LayoutAuth>
    );
  }

  if (enviadoA) {
    return (
      <LayoutAuth titulo="Revisa tu correo" subtitulo="Te enviamos un enlace para autorizar tu registro.">
        <AlertaFormulario texto={errorServidor} />
        <div className="flex flex-col items-center text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-violet-50 text-violet-600">
            <MailCheck className="size-8" aria-hidden="true" />
          </div>
          <p className="mt-4 text-[15px] leading-6 text-gray-600">
            Abre el correo que enviamos a <strong className="break-all text-gray-900">{enviadoA}</strong> y
            toca <strong className="text-gray-900">Autorizar y continuar</strong>. El enlace vence en 30 minutos.
          </p>
          <p className="mt-2 text-sm text-gray-500">Si no lo ves, revisa la carpeta de spam.</p>

          {sesion && (
            <p role="status" className="mt-5 flex items-center gap-2 rounded-xl bg-violet-50 px-4 py-3 text-sm font-semibold text-violet-700">
              <LoaderCircle className="size-4 shrink-0 animate-spin" aria-hidden="true" />
              Esperando tu autorización. Esta página avanzará sola.
            </p>
          )}

          <button
            type="button"
            onClick={() => pedir(enviadoA)}
            disabled={espera > 0 || enviando}
            className="mt-6 flex h-[46px] w-full items-center justify-center gap-2 rounded-xl border border-zinc-200 font-bold text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:text-gray-400"
          >
            {enviando && <LoaderCircle className="size-[18px] animate-spin" aria-hidden="true" />}
            {espera > 0 ? `Reenviar en ${espera} s` : 'Reenviar correo'}
          </button>
          <button
            type="button"
            onClick={empezarDeNuevo}
            className="mt-3 text-sm font-bold text-violet-600 hover:underline"
          >
            Usar otro correo
          </button>
        </div>
      </LayoutAuth>
    );
  }

  return (
    <LayoutAuth titulo="Crear cuenta" subtitulo="Primero confirmemos que el correo es tuyo.">
      <AlertaFormulario texto={errorServidor} />

      <form onSubmit={enviar} noValidate className="flex flex-col gap-4">
        <CampoTexto
          id="email"
          etiqueta="Correo electrónico"
          tipo="email"
          valor={email}
          onChange={(v) => { setEmail(v); setError(''); setErrorServidor(''); }}
          placeholder="nombre@correo.com"
          error={error}
          deshabilitado={enviando}
          autoComplete="email"
          maxLength={TEXTO_USUARIO_MAX}
        />

        <button
          type="submit"
          disabled={enviando}
          className="flex h-[46px] items-center justify-center gap-2 rounded-xl bg-violet-600 text-[15px] font-bold text-white transition hover:bg-violet-700 disabled:cursor-not-allowed disabled:bg-violet-400"
        >
          {enviando ? (
            <>
              <LoaderCircle className="size-[18px] animate-spin" aria-hidden="true" />
              Enviando...
            </>
          ) : (
            'Enviar enlace de autorización'
          )}
        </button>

        <p className="text-center text-sm text-gray-500">
          ¿Ya tienes una cuenta?{' '}
          <Link to="/login" className="font-bold text-violet-600 hover:underline">Inicia sesión</Link>
        </p>
      </form>
    </LayoutAuth>
  );
}
