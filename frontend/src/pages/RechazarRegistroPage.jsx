import { useState } from 'react';
import { CircleCheck, LoaderCircle, ShieldAlert } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import LayoutAuth from '../components/LayoutAuth';
import AlertaFormulario from '../components/AlertaFormulario';
import EnlaceNoValido from '../components/EnlaceNoValido';
import { rechazarRegistro } from '../services/authService';

/**
 * HU8: "No fui yo" desde el correo (/registro/rechazar?token=...). Se pide confirmar con un
 * botón para que abrir el enlace (o que lo abra un antivirus) no haga nada por sí solo.
 */
export default function RechazarRegistroPage() {
  const [parametros] = useSearchParams();
  const token = parametros.get('token') ?? '';
  const [fase, setFase] = useState(token ? 'preguntar' : 'invalido'); // preguntar | listo | invalido
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');
  const [mensajeInvalido, setMensajeInvalido] = useState('');

  const confirmar = async () => {
    setEnviando(true);
    setError('');
    try {
      await rechazarRegistro(token);
      setFase('listo');
    } catch (e) {
      if (e.estado === 410) {
        setMensajeInvalido(e.message);
        setFase('invalido');
      } else {
        setError(e.message);
      }
    } finally {
      setEnviando(false);
    }
  };

  if (fase === 'invalido') {
    return (
      <LayoutAuth titulo="Enlace no válido" subtitulo="Este enlace ya no se puede usar.">
        <EnlaceNoValido mensaje={mensajeInvalido} />
      </LayoutAuth>
    );
  }

  if (fase === 'listo') {
    return (
      <LayoutAuth titulo="Listo" subtitulo="No se creará ninguna cuenta con tu correo.">
        <div className="flex flex-col items-center text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-700">
            <CircleCheck className="size-8" aria-hidden="true" />
          </div>
          <p className="mt-4 text-[15px] leading-6 text-gray-600">
            Anulamos el enlace y no recibirás más correos de registro durante las próximas 24 horas.
          </p>
          <Link to="/login" className="mt-6 text-sm font-bold text-violet-600 hover:underline">
            Ir a Metamorfosis
          </Link>
        </div>
      </LayoutAuth>
    );
  }

  return (
    <LayoutAuth titulo="¿No pediste crear una cuenta?" subtitulo="Alguien escribió tu correo en Metamorfosis.">
      <AlertaFormulario texto={error} />
      <div className="flex flex-col items-center text-center">
        <div className="flex size-16 items-center justify-center rounded-2xl bg-amber-50 text-amber-700">
          <ShieldAlert className="size-8" aria-hidden="true" />
        </div>
        <p className="mt-4 text-[15px] leading-6 text-gray-600">
          Si no fuiste tú, confirma abajo. No se creará ninguna cuenta y el enlace dejará de funcionar.
        </p>
        <button
          type="button"
          onClick={confirmar}
          disabled={enviando}
          className="mt-6 flex h-[46px] w-full items-center justify-center gap-2 rounded-xl bg-violet-600 font-bold text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:bg-violet-400"
        >
          {enviando && <LoaderCircle className="size-[18px] animate-spin" aria-hidden="true" />}
          {enviando ? 'Anulando...' : 'Confirmar que no fui yo'}
        </button>
        <Link
          to={`/registro/autorizar?token=${encodeURIComponent(token)}`}
          className="mt-3 text-sm font-bold text-violet-600 hover:underline"
        >
          Sí fui yo, quiero registrarme
        </Link>
      </div>
    </LayoutAuth>
  );
}
