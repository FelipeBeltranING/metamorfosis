import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { CircleAlert, LoaderCircle, Trophy } from 'lucide-react';
import EncabezadoAuth from '../components/EncabezadoAuth';
import { PLAZOS, TIPOS } from '../constants/opcionesMeta';
import { obtenerLogroPublico } from '../services/logroService';
import { formatearFecha } from '../utils/fechas';

const TIPO_POR_VALOR = Object.fromEntries(TIPOS.map((t) => [t.valor, t.etiqueta]));
const PLAZO_POR_VALOR = Object.fromEntries(PLAZOS.map((p) => [p.valor, p.etiqueta]));

/** HU21: lo que ve quien abre el enlace compartido. Ruta pública, no pide sesión. */
export default function LogroPublicoPage() {
  const { token } = useParams();
  const [logro, setLogro] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let activo = true;
    obtenerLogroPublico(token)
      .then((datos) => {
        if (!activo) return;
        setLogro(datos);
        document.title = `${datos.nombreVisible} cumplió una meta · Metamorfosis`;
      })
      .catch((e) => activo && setError(e.estado === 404 ? 'Este logro no existe o el enlace no es válido.' : e.message));
    return () => {
      activo = false;
    };
  }, [token]);

  const fecha = logro ? formatearFecha(logro.fechaCumplida) : '';

  return (
    <div className="flex min-h-screen flex-col bg-slate-50">
      <EncabezadoAuth />
      <main className="flex flex-1 items-center justify-center px-4 py-10">
        {!logro && !error && (
          <p role="status" className="flex items-center gap-2 text-gray-600">
            <LoaderCircle className="size-5 animate-spin text-violet-600" aria-hidden="true" />
            Cargando logro...
          </p>
        )}

        {error && (
          <section className="w-full max-w-md rounded-2xl bg-white p-8 text-center shadow-sm">
            <div className="mx-auto flex size-14 items-center justify-center rounded-full bg-red-50 text-red-600">
              <CircleAlert className="size-7" aria-hidden="true" />
            </div>
            <h1 className="mt-4 text-xl font-bold text-gray-900">No encontramos este logro</h1>
            <p className="mt-2 text-sm text-gray-600">{error}</p>
            <Link to="/registro" className="mt-6 inline-flex h-11 items-center rounded-xl bg-violet-600 px-5 font-semibold text-white hover:bg-violet-700">
              Conoce Metamorfosis
            </Link>
          </section>
        )}

        {logro && (
          <section className="w-full max-w-md rounded-2xl bg-white p-8 text-center shadow-sm" aria-labelledby="titulo-logro">
            <div className="mx-auto flex size-[72px] items-center justify-center rounded-full bg-violet-50 text-violet-600">
              <Trophy className="size-9" aria-hidden="true" />
            </div>
            <p className="mt-5 text-sm font-semibold uppercase tracking-wide text-violet-700">
              {logro.nombreVisible} cumplió una meta
            </p>
            <h1 id="titulo-logro" className="mt-2 text-2xl font-extrabold text-gray-900">
              {logro.nombreMeta}
            </h1>
            <div className="mt-4 flex flex-wrap justify-center gap-2">
              {TIPO_POR_VALOR[logro.tipo] && (
                <span className="rounded-xl bg-slate-100 px-3 py-1.5 text-[13px] font-semibold text-gray-600">{TIPO_POR_VALOR[logro.tipo]}</span>
              )}
              {PLAZO_POR_VALOR[logro.plazo] && (
                <span className="rounded-xl bg-slate-100 px-3 py-1.5 text-[13px] font-semibold text-gray-600">{PLAZO_POR_VALOR[logro.plazo]}</span>
              )}
              {fecha && (
                <span className="rounded-xl bg-emerald-50 px-3 py-1.5 text-[13px] font-bold text-emerald-800">Cumplida el {fecha}</span>
              )}
            </div>
            <div className="mt-8 border-t border-gray-100 pt-6">
              <p className="text-[15px] text-gray-700">¿Tú también tienes una meta en mente?</p>
              <Link to="/registro" className="mt-4 inline-flex h-12 w-full items-center justify-center rounded-xl bg-violet-600 font-bold text-white hover:bg-violet-700">
                Crear mi cuenta gratis
              </Link>
            </div>
          </section>
        )}
      </main>
    </div>
  );
}
