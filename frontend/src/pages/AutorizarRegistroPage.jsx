import { useEffect, useState } from 'react';
import { CircleCheck, LoaderCircle } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import { Trans, useTranslation } from 'react-i18next';
import i18n from '../i18n';
import LayoutAuth from '../components/LayoutAuth';
import EnlaceNoValido from '../components/EnlaceNoValido';
import { autorizarRegistro } from '../services/authService';
import { CANAL_REGISTRO } from '../constants/registro';

/** Avisa a la pestaña donde se pidió el registro (si está en este mismo navegador). */
function avisarOtrasPestanas() {
  try {
    const canal = new BroadcastChannel(CANAL_REGISTRO);
    canal.postMessage('autorizada');
    canal.close();
  } catch {
    // Navegador sin BroadcastChannel: la otra pestaña igual se entera preguntando cada pocos segundos.
  }
}

/**
 * HU8: destino del botón "Autorizar y continuar" del correo. Autoriza la solicitud y la pestaña
 * donde se pidió el registro avanza sola al formulario. Si se prefiere, se puede seguir aquí.
 */
export default function AutorizarRegistroPage() {
  const { t } = useTranslation();
  const [parametros] = useSearchParams();
  const token = parametros.get('token') ?? '';
  const [estado, setEstado] = useState(() => (token ? { fase: 'autorizando' } : { fase: 'invalido' }));

  useEffect(() => {
    if (!token) return undefined;
    let activo = true;
    autorizarRegistro(token)
      .then((datos) => {
        if (!activo) return;
        setEstado({ fase: 'listo', email: datos?.email });
        avisarOtrasPestanas();
      })
      .catch((e) => activo && setEstado({
        fase: 'invalido',
        mensaje: e.estado === 410 ? e.message : i18n.t('auth.autorizar.error'),
      }));
    return () => {
      activo = false;
    };
  }, [token]);

  if (estado.fase === 'autorizando') {
    return (
      <LayoutAuth titulo={t('auth.autorizar.autorizandoTitulo')} subtitulo={t('auth.autorizar.autorizandoSubtitulo')}>
        <p role="status" className="flex items-center justify-center gap-2 py-8 text-sm text-gray-600">
          <LoaderCircle className="size-5 animate-spin text-violet-600" aria-hidden="true" />
          {t('auth.autorizar.autorizando')}
        </p>
      </LayoutAuth>
    );
  }

  if (estado.fase === 'invalido') {
    return (
      <LayoutAuth titulo={t('auth.autorizar.invalidoTitulo')} subtitulo={t('auth.autorizar.invalidoSubtitulo')}>
        <EnlaceNoValido mensaje={estado.mensaje} />
      </LayoutAuth>
    );
  }

  return (
    <LayoutAuth titulo={t('auth.autorizar.listoTitulo')} subtitulo={t('auth.autorizar.listoSubtitulo')}>
      <div className="flex flex-col items-center text-center">
        <div className="flex size-16 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-700">
          <CircleCheck className="size-8" aria-hidden="true" />
        </div>
        <p className="mt-4 text-[15px] leading-6 text-gray-600">
          <Trans i18nKey="auth.autorizar.listoTexto" components={{ b: <strong className="text-gray-900" /> }} />
        </p>
        {estado.email && (
          <p className="mt-2 break-all text-sm text-gray-500">{estado.email}</p>
        )}
        <Link
          to={`/registro/completar?token=${encodeURIComponent(token)}`}
          className="mt-6 flex h-[46px] w-full items-center justify-center rounded-xl border border-zinc-200 font-bold text-gray-700 hover:bg-gray-50"
        >
          {t('auth.autorizar.continuarAqui')}
        </Link>
      </div>
    </LayoutAuth>
  );
}
