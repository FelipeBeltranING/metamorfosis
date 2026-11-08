import { useState } from 'react';
import { Lock } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { cambiarPrivacidad } from '../services/perfilService';

const AVISOS = {
  privado: 'Tu perfil ahora es privado.',
  publico: 'Tu perfil vuelve a ser público.',
};

/**
 * HU26: interruptor Perfil privado de Mi perfil. Se guarda al instante; si falla, vuelve a su
 * estado anterior y muestra el error. Con el perfil privado los demás solo ven la cabecera.
 */
export default function TarjetaPrivacidad({ inicial = false }) {
  const navigate = useNavigate();
  const [privado, setPrivado] = useState(Boolean(inicial));
  const [guardando, setGuardando] = useState(false);
  const [aviso, setAviso] = useState(null);

  const alternar = async () => {
    if (guardando) return;
    const anterior = privado;
    setPrivado(!anterior);
    setGuardando(true);
    setAviso(null);
    try {
      const { privado: guardado } = await cambiarPrivacidad(!anterior);
      setPrivado(guardado);
      setAviso({ tipo: 'ok', texto: guardado ? AVISOS.privado : AVISOS.publico });
    } catch (error) {
      setPrivado(anterior);
      if (error.estado === 401) navigate('/login', { replace: true });
      else setAviso({ tipo: 'error', texto: error.message });
    } finally {
      setGuardando(false);
    }
  };

  return (
    <section aria-labelledby="perfil-privacidad" className="rounded-2xl border border-zinc-200 bg-white p-5 sm:p-6">
      <div className="flex items-center gap-4">
        <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-violet-100">
          <Lock className="size-6 text-violet-600" aria-hidden="true" />
        </div>
        <div className="min-w-0 flex-1">
          <h2 id="perfil-privacidad" className="text-lg font-bold text-gray-900">Perfil privado</h2>
          <p id="perfil-privacidad-ayuda" className="text-sm text-gray-600">
            Si lo activas, las demás personas solo verán tu cabecera: foto, nombre, frase, portada y desde cuándo eres miembro.
          </p>
        </div>
        <button
          type="button"
          role="switch"
          aria-checked={privado}
          aria-labelledby="perfil-privacidad"
          aria-describedby="perfil-privacidad-ayuda"
          disabled={guardando}
          onClick={alternar}
          className={`relative inline-flex h-8 w-14 shrink-0 items-center rounded-full transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300 focus-visible:ring-offset-2 disabled:opacity-60 ${privado ? 'bg-violet-600' : 'bg-zinc-300'}`}
        >
          <span
            className={`inline-block size-6 rounded-full bg-white shadow transition-transform ${privado ? 'translate-x-7' : 'translate-x-1'}`}
            aria-hidden="true"
          />
        </button>
      </div>
      <p
        role={aviso?.tipo === 'error' ? 'alert' : 'status'}
        className={`mt-3 text-sm font-semibold ${aviso?.tipo === 'error' ? 'text-red-700' : 'text-emerald-700'} ${aviso ? '' : 'sr-only'}`}
      >
        {aviso?.texto ?? ''}
      </p>
    </section>
  );
}
