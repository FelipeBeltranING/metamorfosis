import { useEffect, useState } from 'react';
import { Check, CircleAlert, Copy, Download, Image as IconoImagen, LoaderCircle, Share2, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import i18n from '../i18n';
import { crearEnlace } from '../services/logroService';
import { obtenerSesion } from '../services/sesionService';
import { generarTarjetaLogro } from '../utils/tarjetaLogro';

/** "Laura" + "Gómez" → "Laura G.", igual que en el enlace público. */
function nombreVisible() {
  const sesion = obtenerSesion();
  const nombre = sesion?.nombre?.trim() ?? '';
  const apellido = sesion?.apellido?.trim() ?? '';
  return apellido ? `${nombre} ${apellido[0]}.` : nombre;
}

/** Nombre de archivo seguro: "Leer 20 libros" → "logro-leer-20-libros.png". */
function nombreArchivo(nombreMeta) {
  const base = nombreMeta
    .normalize('NFD').replace(/[̀-ͯ]/g, '')
    .toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-+|-+$/g, '')
    .slice(0, 40);
  return `${i18n.t('logro.prefijoArchivo')}-${base || 'metamorfosis'}.png`;
}

/**
 * HU21: compartir un logro como imagen. Crea (o reutiliza) el enlace público, dibuja la
 * tarjeta del logro y la ofrece para compartir desde el celular, descargar o copiar.
 */
export default function CompartirLogroModal({ meta, onCerrar }) {
  const { t } = useTranslation();
  const [datos, setDatos] = useState(null); // { url, archivo, vistaPrevia }
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let activo = true;
    let vistaPrevia = null;
    (async () => {
      try {
        const enlace = await crearEnlace(meta.codigoMeta);
        const blob = await generarTarjetaLogro({
          nombreMeta: meta.nombre,
          nombreVisible: nombreVisible(),
          fechaCumplida: enlace.fechaCumplida ?? meta.fechaCumplida,
        });
        if (!activo) return;
        vistaPrevia = URL.createObjectURL(blob);
        const archivo = new File([blob], nombreArchivo(meta.nombre), { type: 'image/png' });
        setDatos({ url: enlace.url, archivo, vistaPrevia });
      } catch (e) {
        if (activo) setError(e.errores?.[0] ?? e.message);
      } finally {
        if (activo) setCargando(false);
      }
    })();
    return () => {
      activo = false;
      if (vistaPrevia) URL.revokeObjectURL(vistaPrevia);
    };
  }, [meta.codigoMeta, meta.nombre, meta.fechaCumplida, intento]);

  // Los avisos ("Imagen copiada") se ocultan solos.
  useEffect(() => {
    if (!aviso) return undefined;
    const temporizador = setTimeout(() => setAviso(''), 2500);
    return () => clearTimeout(temporizador);
  }, [aviso]);

  const reintentar = () => {
    setError('');
    setCargando(true);
    setIntento((n) => n + 1);
  };

  const texto = t('logro.textoCompartir', { meta: meta.nombre });
  const puedeCompartirImagen = Boolean(datos)
    && typeof navigator.canShare === 'function'
    && navigator.canShare({ files: [datos.archivo] });
  const puedeCopiarImagen = typeof window.ClipboardItem === 'function' && Boolean(navigator.clipboard?.write);

  const compartirImagen = async () => {
    try {
      await navigator.share({ files: [datos.archivo], title: 'Metamorfosis', text: `${texto} ${datos.url}` });
    } catch (e) {
      // Cancelar el menú del sistema no es un error.
      if (e?.name !== 'AbortError') setError(t('logro.errorCompartir'));
    }
  };

  const copiarImagen = async () => {
    try {
      await navigator.clipboard.write([new window.ClipboardItem({ 'image/png': datos.archivo })]);
      setAviso(t('logro.imagenCopiada'));
    } catch {
      setError(t('logro.errorCopiarImagen'));
    }
  };

  const copiarEnlace = async () => {
    try {
      await navigator.clipboard.writeText(datos.url);
      setAviso(t('logro.enlaceCopiado'));
    } catch {
      setError(t('logro.errorCopiarEnlace'));
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 sm:items-center sm:p-4"
      role="presentation"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onCerrar();
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-compartir-logro"
        className="relative max-h-[95vh] w-full max-w-lg overflow-y-auto rounded-t-3xl bg-white px-5 pb-7 pt-3 shadow-2xl sm:rounded-2xl sm:p-6"
      >
        <div aria-hidden="true" className="mx-auto h-1 w-10 rounded-full bg-gray-300 sm:hidden" />
        <div className="mt-4 flex items-start justify-between sm:mt-0">
          <h2 id="titulo-compartir-logro" className="text-xl font-bold text-gray-900">
            {t('logro.titulo')}
          </h2>
          <button
            type="button"
            onClick={onCerrar}
            aria-label={t('logro.cerrarVentana')}
            className="rounded-lg p-2 text-gray-500 hover:bg-gray-100"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>

        {cargando && (
          <div role="status" className="mt-4 flex aspect-[1200/630] w-full flex-col items-center justify-center gap-2 rounded-xl bg-violet-50 text-sm text-violet-700">
            <LoaderCircle className="size-6 animate-spin" aria-hidden="true" />
            {t('logro.preparando')}
          </div>
        )}

        {!cargando && !datos && error && (
          <div className="mt-6">
            <div role="alert" className="flex gap-2 rounded-xl bg-red-50 p-3 text-sm text-red-700">
              <CircleAlert className="size-5 shrink-0" aria-hidden="true" />
              <p>{error}</p>
            </div>
            <button
              type="button"
              onClick={reintentar}
              className="mt-4 h-11 w-full rounded-xl border border-gray-200 font-semibold text-gray-700 hover:bg-gray-50"
            >
              {t('logro.reintentar')}
            </button>
          </div>
        )}

        {datos && (
          <>
            <img
              src={datos.vistaPrevia}
              alt={t('logro.altTarjeta', { meta: meta.nombre })}
              width={1200}
              height={630}
              className="mt-4 w-full rounded-xl shadow-sm ring-1 ring-black/5"
            />

            {puedeCompartirImagen && (
              <button
                type="button"
                onClick={compartirImagen}
                className="mt-5 flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-violet-600 font-bold text-white hover:bg-violet-700"
              >
                <Share2 className="size-5" aria-hidden="true" />
                {t('logro.compartirImagen')}
              </button>
            )}

            <div className={`grid gap-3 ${puedeCompartirImagen ? 'mt-3' : 'mt-5'} ${puedeCopiarImagen ? 'grid-cols-2' : 'grid-cols-1'}`}>
              <a
                href={datos.vistaPrevia}
                download={datos.archivo.name}
                className={`flex h-12 items-center justify-center gap-2 rounded-xl font-bold ${
                  puedeCompartirImagen
                    ? 'border border-gray-200 text-gray-700 hover:bg-gray-50'
                    : 'bg-violet-600 text-white hover:bg-violet-700'
                }`}
              >
                <Download className="size-5" aria-hidden="true" />
                {t('logro.descargar')}
              </a>
              {puedeCopiarImagen && (
                <button
                  type="button"
                  onClick={copiarImagen}
                  className="flex h-12 items-center justify-center gap-2 rounded-xl border border-gray-200 font-bold text-gray-700 hover:bg-gray-50"
                >
                  <IconoImagen className="size-5" aria-hidden="true" />
                  {t('logro.copiarImagen')}
                </button>
              )}
            </div>

            <p className="mt-5 text-sm font-semibold text-gray-700">{t('logro.oEnlace')}</p>
            <div className="mt-1.5 flex items-center gap-2 rounded-xl bg-slate-50 p-2 pl-3">
              <span className="min-w-0 flex-1 truncate text-sm text-gray-600" title={datos.url}>
                {datos.url}
              </span>
              <button
                type="button"
                onClick={copiarEnlace}
                className="flex h-9 shrink-0 items-center gap-1.5 rounded-lg bg-white px-3 text-sm font-semibold text-violet-700 shadow-sm hover:bg-violet-50"
              >
                {aviso === t('logro.enlaceCopiado') ? <Check className="size-4" aria-hidden="true" /> : <Copy className="size-4" aria-hidden="true" />}
                {aviso === t('logro.enlaceCopiado') ? t('logro.copiado') : t('logro.copiar')}
              </button>
            </div>

            <p role="status" aria-live="polite" className="mt-3 min-h-5 text-center text-sm font-semibold text-emerald-700">
              {aviso}
            </p>
            {error && <p role="alert" className="text-center text-sm text-red-700">{error}</p>}

            <p className="mt-2 text-center text-xs text-gray-500">
              {t('logro.privacidad')}
            </p>
          </>
        )}
      </section>
    </div>
  );
}
