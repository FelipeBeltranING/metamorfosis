import { useEffect, useState } from 'react';
import {
  ArrowRight, CircleCheck, Clock, LoaderCircle, MailOpen, Users,
} from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import Encabezado from '../components/Encabezado';
import EstadoError from '../components/EstadoError';
import AvatarUsuario from '../components/AvatarUsuario';
import { listarInvitaciones, responderInvitacion } from '../services/invitacionService';
import { inicialesDe } from '../services/sesionService';
import { PLAZOS, TIPOS } from '../constants/opcionesMeta';

const TIPO_POR_VALOR = Object.fromEntries(TIPOS.map((t) => [t.valor, t]));
const PLAZO_POR_VALOR = Object.fromEntries(PLAZOS.map((p) => [p.valor, p]));

/** "hace 2 días", "hoy"… a partir de la fecha de la invitación. */
function haceCuanto(fechaIso) {
  if (!fechaIso) return '';
  const dias = Math.floor((Date.now() - new Date(fechaIso).getTime()) / 86400000);
  if (dias <= 0) return 'hoy';
  if (dias === 1) return 'ayer';
  return `hace ${dias} días`;
}

function TarjetaInvitacion({ invitacion, onResponder }) {
  const [enviando, setEnviando] = useState(null);
  const [error, setError] = useState('');
  const tipo = TIPO_POR_VALOR[invitacion.tipo] ?? TIPO_POR_VALOR.OTRO;
  const plazo = PLAZO_POR_VALOR[invitacion.plazo];
  const { invitador } = invitacion;

  const responder = async (aceptar) => {
    setEnviando(aceptar ? 'aceptar' : 'rechazar');
    setError('');
    try {
      await onResponder(invitacion, aceptar);
    } catch (e) {
      setError(e.message);
      setEnviando(null);
    }
  };

  return (
    <li className="flex flex-col gap-4 rounded-2xl border border-zinc-200 bg-white p-5 sm:p-6">
      <div className="flex items-center gap-3">
        <AvatarUsuario foto={invitador.foto} iniciales={inicialesDe(invitador.nombre, invitador.apellido)} clase="size-11 text-sm" />
        <p className="min-w-0 flex-1 text-[15px] text-gray-600">
          <strong className="font-bold text-gray-900">{invitador.nombre} {invitador.apellido}</strong> te invitó a su meta
          <span className="block text-[13px] text-gray-500">{haceCuanto(invitacion.fechaInvitacion)}</span>
        </p>
      </div>
      <div className="flex items-center gap-3 rounded-xl bg-violet-50 p-4">
        <div className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-white">
          <tipo.Icono className="size-6 text-violet-600" aria-hidden="true" />
        </div>
        <div className="min-w-0">
          <h2 className="break-words text-lg font-extrabold text-gray-900">{invitacion.nombreMeta}</h2>
          <p className="text-[13px] font-semibold text-gray-600">
            {tipo.etiqueta}{plazo ? ` · ${plazo.etiqueta}` : ''}
          </p>
        </div>
      </div>
      <p className="flex items-center gap-2 text-sm text-gray-600">
        <Users className="size-4 shrink-0" aria-hidden="true" />
        {invitacion.participantes === 1 ? 'Por ahora solo participa quien la creó' : `${invitacion.participantes} personas participan`}
        {' · '}compartirán el camino y cada uno marcará sus días.
      </p>
      {error && <p role="alert" className="text-sm font-semibold text-red-600">{error}</p>}
      <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <button
          type="button"
          onClick={() => responder(false)}
          disabled={enviando !== null}
          className="flex h-11 items-center justify-center gap-2 rounded-xl border border-zinc-300 bg-white px-5 font-bold text-gray-700 hover:bg-gray-50 disabled:opacity-60"
        >
          {enviando === 'rechazar' && <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />}
          Rechazar
        </button>
        <button
          type="button"
          onClick={() => responder(true)}
          disabled={enviando !== null}
          className="flex h-11 items-center justify-center gap-2 rounded-xl bg-violet-600 px-5 font-bold text-white hover:bg-violet-700 disabled:opacity-60"
        >
          {enviando === 'aceptar' && <LoaderCircle className="size-4 animate-spin" aria-hidden="true" />}
          Aceptar invitación
        </button>
      </div>
    </li>
  );
}

/**
 * HU14: invitaciones pendientes a metas compartidas. Al aceptar, la meta pasa a Mis metas;
 * al rechazar, desaparece. También se puede responder desde los botones del correo.
 */
export default function InvitacionesPage() {
  const navigate = useNavigate();
  const [estado, setEstado] = useState({ fase: 'cargando' });
  const [intento, setIntento] = useState(0);
  const [aviso, setAviso] = useState(null); // { aceptada, codigoMeta, nombreMeta }

  useEffect(() => {
    let activo = true;
    listarInvitaciones()
      .then((invitaciones) => activo && setEstado({ fase: 'lista', invitaciones }))
      .catch((error) => {
        if (!activo) return;
        if (error.estado === 401) navigate('/login', { replace: true });
        else setEstado({ fase: 'error', mensaje: error.message });
      });
    return () => { activo = false; };
  }, [intento, navigate]);

  const responder = async (invitacion, aceptar) => {
    try {
      const r = await responderInvitacion(invitacion.codigoMeta, aceptar);
      setEstado((actual) => ({ ...actual, invitaciones: actual.invitaciones.filter((i) => i.codigoMeta !== invitacion.codigoMeta) }));
      setAviso(r);
      window.dispatchEvent(new Event('metamorfosis:invitaciones'));
    } catch (error) {
      if (error.estado === 401) navigate('/login', { replace: true });
      else throw error;
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <Encabezado />
      <main className="mx-auto flex w-full max-w-[720px] flex-col gap-5 px-4 pb-16 pt-6 sm:pt-12" aria-busy={estado.fase === 'cargando'}>
        <div>
          <h1 className="text-2xl font-bold sm:text-3xl">Invitaciones</h1>
          <p className="mt-1 text-[15px] text-gray-600">Metas compartidas a las que te invitaron.</p>
        </div>

        {aviso && (
          <div role="status" className={`flex flex-wrap items-center gap-3 rounded-2xl p-4 text-[15px] font-semibold ${aviso.aceptada ? 'bg-emerald-50 text-emerald-800' : 'bg-slate-100 text-gray-700'}`}>
            <CircleCheck className="size-5 shrink-0" aria-hidden="true" />
            <span className="flex-1">
              {aviso.aceptada ? `¡Te uniste a «${aviso.nombreMeta}»! Ya está en tus metas.` : `Rechazaste la invitación a «${aviso.nombreMeta}».`}
            </span>
            {aviso.aceptada && (
              <Link to={`/metas/${aviso.codigoMeta}`} className="inline-flex items-center gap-1 font-bold underline-offset-2 hover:underline">
                Ver la meta <ArrowRight className="size-4" aria-hidden="true" />
              </Link>
            )}
          </div>
        )}

        {estado.fase === 'cargando' && (
          <div className="flex animate-pulse flex-col gap-4" aria-hidden="true">
            {[1, 2].map((n) => <div key={n} className="h-64 rounded-2xl bg-slate-200" />)}
          </div>
        )}
        {estado.fase === 'error' && (
          <EstadoError
            titulo="No pudimos cargar tus invitaciones"
            mensaje={estado.mensaje}
            onReintentar={() => { setEstado({ fase: 'cargando' }); setIntento((n) => n + 1); }}
          />
        )}
        {estado.fase === 'lista' && estado.invitaciones.length === 0 && (
          <section className="flex flex-col items-center gap-3 rounded-2xl border border-dashed border-zinc-300 bg-white px-6 py-12 text-center">
            <div className="flex size-16 items-center justify-center rounded-2xl bg-violet-50 text-violet-600">
              <MailOpen className="size-8" aria-hidden="true" />
            </div>
            <h2 className="text-xl font-extrabold">No tienes invitaciones pendientes</h2>
            <p className="max-w-md text-[15px] leading-6 text-gray-600">
              Cuando alguien te invite a una meta, aparecerá aquí y te llegará un correo.
            </p>
            <p className="text-[15px] text-gray-600">¿Te compartieron un código? Úsalo en Mis metas, con «Unirme con código».</p>
            <Link to="/metas" className="mt-1 font-bold text-violet-700 hover:underline">Ir a mis metas</Link>
          </section>
        )}
        {estado.fase === 'lista' && estado.invitaciones.length > 0 && (
          <>
            <p className="flex items-center gap-2 text-sm font-semibold text-gray-600">
              <Clock className="size-4" aria-hidden="true" />
              {estado.invitaciones.length === 1 ? '1 invitación pendiente' : `${estado.invitaciones.length} invitaciones pendientes`}
            </p>
            <ul className="flex flex-col gap-4">
              {estado.invitaciones.map((i) => <TarjetaInvitacion key={i.codigoMeta} invitacion={i} onResponder={responder} />)}
            </ul>
          </>
        )}
      </main>
    </div>
  );
}
