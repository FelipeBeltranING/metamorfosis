import { useEffect, useState } from 'react';
import { ArrowLeft, UserX } from 'lucide-react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import Encabezado from '../components/Encabezado';
import EstadoError from '../components/EstadoError';
import { ContenidoPerfil, EsqueletoPerfil } from './PerfilPage';
import { obtenerPerfilPublico } from '../services/perfilService';
import { obtenerSesion } from '../services/sesionService';

const ES_CODIGO = /^\d+$/;

/**
 * HU24: perfil público de otra persona. Reutiliza los bloques de Mi perfil (HU22) sin el correo
 * ni las acciones propias; las metas que el dueño ocultó (HU25) no llegan del servidor.
 */
export default function PerfilPublicoPage() {
  const { t } = useTranslation();
  const { codigoUsuario } = useParams();
  const navigate = useNavigate();
  const propio = String(obtenerSesion()?.codigoUsuario ?? '') === codigoUsuario;
  const valido = ES_CODIGO.test(codigoUsuario ?? '');
  const [intento, setIntento] = useState(0);
  // Respuesta del servidor, marcada con la clave (código + intento) que la pidió.
  const [respuesta, setRespuesta] = useState({ clave: null });
  const clave = `${codigoUsuario}|${intento}`;

  useEffect(() => {
    if (propio || !valido) return undefined;
    let activo = true;
    obtenerPerfilPublico(codigoUsuario)
      .then((perfil) => activo && setRespuesta({ clave, fase: 'listo', perfil }))
      .catch((error) => {
        if (!activo) return;
        if (error.estado === 401) navigate('/login', { replace: true });
        else if (error.estado === 404) setRespuesta({ clave, fase: 'noExiste' });
        else setRespuesta({ clave, fase: 'error', mensaje: error.message });
      });
    return () => {
      activo = false;
    };
  }, [codigoUsuario, propio, valido, clave, navigate]);

  // Su propio perfil público se ve en Mi perfil.
  if (propio) return <Navigate to="/perfil" replace />;

  let estado;
  if (!valido) estado = { fase: 'noExiste' };
  else if (respuesta.clave !== clave) estado = { fase: 'cargando' };
  else estado = respuesta;
  const perfil = estado.fase === 'listo' ? estado.perfil : null;

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />
      <main className="mx-auto w-full max-w-[860px] px-4 pb-16 pt-6 sm:pt-12" aria-busy={estado.fase === 'cargando'}>
        <Link
          to="/usuarios"
          className="mb-4 inline-flex items-center gap-2 rounded-md text-sm font-bold text-violet-700 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
        >
          <ArrowLeft className="size-4" aria-hidden="true" />
          {t('perfilPublico.buscarPersonas')}
        </Link>
        <h1 className="mb-6 text-2xl font-bold sm:text-3xl">
          {perfil ? t('perfilPublico.perfilDe', { nombre: perfil.nombre }) : t('perfilPublico.perfil')}
        </h1>

        {estado.fase === 'cargando' && <EsqueletoPerfil />}

        {estado.fase === 'error' && (
          <EstadoError
            titulo={t('perfilPublico.errorCargar')}
            mensaje={estado.mensaje}
            onReintentar={() => setIntento((n) => n + 1)}
          />
        )}

        {estado.fase === 'noExiste' && (
          <div className="flex flex-col items-center gap-4 rounded-2xl border border-zinc-200 bg-white p-8 text-center">
            <div className="flex size-14 items-center justify-center rounded-2xl bg-violet-50">
              <UserX className="size-7 text-violet-600" aria-hidden="true" />
            </div>
            <div>
              <p className="text-lg font-bold text-gray-900">{t('perfilPublico.noExiste')}</p>
              <p className="mt-1 text-sm text-gray-600">{t('perfilPublico.noExisteTexto')}</p>
            </div>
            <Link
              to="/usuarios"
              className="flex h-12 items-center rounded-xl bg-violet-600 px-6 font-bold text-white hover:bg-violet-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
            >
              {t('perfilPublico.buscarPersonas')}
            </Link>
          </div>
        )}

        {perfil && <ContenidoPerfil perfil={perfil} publico />}
      </main>
    </div>
  );
}
