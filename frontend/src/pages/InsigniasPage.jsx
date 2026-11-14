import { useEffect, useState } from 'react';
import { ArrowLeft, CircleCheck, Lock } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { Trans, useTranslation } from 'react-i18next';
import Encabezado from '../components/Encabezado';
import EstadoError from '../components/EstadoError';
import IconoInsignia from '../components/IconoInsignia';
import { obtenerGaleria } from '../services/insigniaService';
import { formatearFecha } from '../utils/fechas';
import {
  comoGanarla, descripcionInsignia, nombreInsignia, porcentajeAvance,
} from '../utils/insignias';

const TARJETA = 'rounded-2xl border border-zinc-200 bg-white p-5 sm:p-6';

/** Barra de avance accesible: el lector de pantalla anuncia "3 de 5". */
function BarraAvance({ valor, maximo, etiqueta }) {
  const { t } = useTranslation();
  return (
    <div
      role="progressbar"
      aria-label={etiqueta}
      aria-valuemin={0}
      aria-valuemax={maximo}
      aria-valuenow={valor}
      aria-valuetext={t('insignias.avance', { valor, maximo })}
      className="h-2 w-full overflow-hidden rounded-full bg-slate-200"
    >
      <div className="h-full rounded-full bg-violet-600" style={{ width: `${porcentajeAvance(valor, maximo)}%` }} />
    </div>
  );
}

function TarjetaInsignia({ insignia }) {
  const { t } = useTranslation();
  const { obtenida } = insignia;
  const nombre = nombreInsignia(insignia);
  return (
    <li
      aria-label={t('insignias.estado', { nombre, estado: obtenida ? t('insignias.ganada') : t('insignias.pendiente') })}
      className={`flex h-full flex-col items-center gap-3 rounded-2xl border p-5 text-center ${
        obtenida ? 'border-violet-200 bg-violet-50/60' : 'border-zinc-200 bg-white'
      }`}
    >
      <div className="relative">
        <div className={`flex size-16 items-center justify-center rounded-full ${obtenida ? 'bg-violet-600' : 'bg-slate-200'}`}>
          <IconoInsignia nombre={insignia.icono} className={`size-8 ${obtenida ? 'text-white' : 'text-slate-400'}`} aria-hidden="true" />
        </div>
        {!obtenida && (
          <span className="absolute -bottom-1 -right-1 flex size-7 items-center justify-center rounded-full border-2 border-white bg-slate-500">
            <Lock className="size-3.5 text-white" aria-hidden="true" />
          </span>
        )}
      </div>
      <p className={`text-base font-bold leading-tight ${obtenida ? 'text-gray-900' : 'text-gray-600'}`}>{nombre}</p>

      {obtenida ? (
        <>
          <p className="text-sm text-gray-600">{descripcionInsignia(insignia)}</p>
          <p className="mt-auto flex items-center gap-1.5 text-sm font-semibold text-emerald-700">
            <CircleCheck className="size-4" aria-hidden="true" />
            {t('insignias.ganadaEl', { fecha: formatearFecha(insignia.fechaObtenida) })}
          </p>
        </>
      ) : (
        <>
          <p className="text-sm text-gray-500">{comoGanarla(insignia)}</p>
          <div className="mt-auto flex w-full flex-col gap-1.5">
            <BarraAvance valor={insignia.avance} maximo={insignia.cantidadRequerida} etiqueta={t('insignias.avanceDe', { nombre })} />
            <p className="text-xs font-semibold text-gray-500">{t('insignias.avance', { valor: insignia.avance, maximo: insignia.cantidadRequerida })}</p>
          </div>
        </>
      )}
    </li>
  );
}

function Esqueleto() {
  return (
    <div className="flex animate-pulse flex-col gap-4" aria-hidden="true">
      <div className={`${TARJETA} flex flex-col gap-3`}>
        <div className="h-5 w-56 rounded bg-slate-200" />
        <div className="h-2 w-full rounded-full bg-slate-200" />
      </div>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {[1, 2, 3, 4, 5, 6].map((n) => <div key={n} className="h-52 rounded-2xl bg-slate-200" />)}
      </div>
    </div>
  );
}

/**
 * HU12: galería de insignias. Las seis del catálogo (HU11) en su orden: las ganadas a color con su
 * fecha y las pendientes en gris, con candado, cómo ganarlas y el avance. Se abre desde Mi perfil.
 */
export default function InsigniasPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [estado, setEstado] = useState({ fase: 'cargando' });
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let activo = true;
    obtenerGaleria()
      .then((galeria) => activo && setEstado({ fase: 'listo', galeria }))
      .catch((error) => {
        if (!activo) return;
        if (error.estado === 401) navigate('/login', { replace: true });
        else setEstado({ fase: 'error', mensaje: error.message });
      });
    return () => {
      activo = false;
    };
  }, [intento, navigate]);

  const galeria = estado.fase === 'listo' ? estado.galeria : null;

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />
      <main className="mx-auto w-full max-w-[960px] px-4 pb-16 pt-6 sm:pt-12" aria-busy={estado.fase === 'cargando'}>
        <Link
          to="/perfil"
          className="mb-3 inline-flex items-center gap-1.5 rounded-md text-sm font-semibold text-gray-600 hover:text-violet-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300"
        >
          <ArrowLeft className="size-4" aria-hidden="true" />
          {t('insignias.miPerfil')}
        </Link>
        <h1 className="mb-6 text-2xl font-bold sm:text-3xl">{t('insignias.titulo')}</h1>

        {estado.fase === 'cargando' && <Esqueleto />}

        {estado.fase === 'error' && (
          <EstadoError
            titulo={t('insignias.errorCargar')}
            mensaje={estado.mensaje}
            onReintentar={() => {
              setEstado({ fase: 'cargando' });
              setIntento((n) => n + 1);
            }}
          />
        )}

        {galeria && (
          <div className="flex flex-col gap-4">
            <section aria-label={t('insignias.resumen')} className={`${TARJETA} flex flex-col gap-3`}>
              <p className="text-lg font-bold">
                {t('insignias.hasGanado', { ganadas: galeria.ganadas, total: galeria.total })}
              </p>
              <BarraAvance valor={galeria.ganadas} maximo={galeria.total} etiqueta={t('insignias.ganadas')} />
              {galeria.ganadas === 0 && (
                <p className="text-sm text-gray-600">
                  <Trans i18nKey="insignias.primera" components={{ b: <strong className="font-semibold text-gray-800" /> }} />
                </p>
              )}
            </section>

            <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3" aria-label={t('insignias.galeria')}>
              {galeria.insignias.map((insignia) => (
                <TarjetaInsignia key={insignia.codigoInsignia} insignia={insignia} />
              ))}
            </ul>
          </div>
        )}
      </main>
    </div>
  );
}
