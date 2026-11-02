import { useEffect, useState } from 'react';
import { Check, CircleAlert, Copy, LoaderCircle, Mail, MessageCircle, Send, Share2, X } from 'lucide-react';
import { crearEnlace } from '../services/logroService';

/**
 * HU21: compartir un logro. Pide al backend el enlace público (lo crea o reutiliza) y ofrece
 * WhatsApp, Telegram, correo, copiar y el menú nativo del dispositivo. El mensaje es editable.
 */
export default function CompartirLogroModal({ meta, onCerrar }) {
  const [enlace, setEnlace] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [mensaje, setMensaje] = useState(`¡Cumplí mi meta "${meta.nombre}" en Metamorfosis! 🦋`);
  const [copiado, setCopiado] = useState(false);
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    let activo = true;
    crearEnlace(meta.codigoMeta)
      .then((datos) => activo && setEnlace(datos.url))
      .catch((e) => activo && setError(e.errores?.[0] ?? e.message))
      .finally(() => activo && setCargando(false));
    return () => {
      activo = false;
    };
  }, [meta.codigoMeta, intento]);

  const reintentar = () => {
    setError('');
    setCargando(true);
    setIntento((n) => n + 1);
  };

  const textoCompleto = `${mensaje} ${enlace ?? ''}`.trim();
  const texto = encodeURIComponent(textoCompleto);
  const opciones = enlace
    ? [
        { nombre: 'WhatsApp', Icono: MessageCircle, href: `https://wa.me/?text=${texto}`, color: 'bg-emerald-50 text-emerald-700' },
        { nombre: 'Telegram', Icono: Send, href: `https://t.me/share/url?url=${encodeURIComponent(enlace)}&text=${encodeURIComponent(mensaje)}`, color: 'bg-sky-50 text-sky-700' },
        { nombre: 'Correo', Icono: Mail, href: `mailto:?subject=${encodeURIComponent('¡Cumplí una meta!')}&body=${texto}`, color: 'bg-amber-50 text-amber-700' },
      ]
    : [];

  const copiar = async () => {
    try {
      await navigator.clipboard.writeText(textoCompleto);
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2000);
    } catch {
      setError('No se pudo copiar. Selecciona el enlace y cópialo manualmente.');
    }
  };

  const puedeCompartirNativo = typeof navigator !== 'undefined' && typeof navigator.share === 'function';
  const compartirNativo = async () => {
    try {
      await navigator.share({ title: 'Metamorfosis', text: mensaje, url: enlace });
    } catch {
      // El usuario canceló el menú del sistema: no es un error.
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/50 sm:items-center sm:p-4"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onCerrar();
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-compartir-logro"
        className="relative w-full max-w-md rounded-t-3xl bg-white px-5 pb-7 pt-3 shadow-2xl sm:rounded-2xl sm:p-6"
      >
        <div aria-hidden="true" className="mx-auto h-1 w-10 rounded-full bg-gray-300 sm:hidden" />
        <div className="mt-4 flex items-start justify-between sm:mt-0">
          <h2 id="titulo-compartir-logro" className="text-xl font-bold text-gray-900">
            Compartir logro
          </h2>
          <button
            type="button"
            onClick={onCerrar}
            aria-label="Cerrar ventana"
            className="rounded-lg p-2 text-gray-500 hover:bg-gray-100"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>

        {cargando && (
          <p className="mt-6 flex items-center justify-center gap-2 py-8 text-sm text-gray-600" role="status">
            <LoaderCircle className="size-5 animate-spin text-violet-600" aria-hidden="true" />
            Preparando tu enlace...
          </p>
        )}

        {!cargando && error && !enlace && (
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
              Intentar de nuevo
            </button>
          </div>
        )}

        {enlace && (
          <>
            <label htmlFor="mensaje-logro" className="mt-5 block text-sm font-semibold text-gray-700">
              Mensaje
            </label>
            <textarea
              id="mensaje-logro"
              value={mensaje}
              onChange={(e) => setMensaje(e.target.value)}
              maxLength={280}
              rows={3}
              className="mt-1.5 w-full resize-none rounded-xl border border-gray-200 p-3 text-[15px] text-gray-800 focus:border-violet-500 focus:outline-none focus:ring-2 focus:ring-violet-200"
            />

            <div className="mt-4 grid grid-cols-3 gap-3">
              {opciones.map(({ nombre, Icono, href, color }) => (
                <a
                  key={nombre}
                  href={href}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex flex-col items-center gap-1.5 rounded-xl border border-gray-100 py-3 text-[13px] font-semibold text-gray-700 hover:bg-gray-50"
                >
                  <span className={`flex size-10 items-center justify-center rounded-full ${color}`}>
                    <Icono className="size-5" aria-hidden="true" />
                  </span>
                  {nombre}
                </a>
              ))}
            </div>

            <div className="mt-4 flex items-center gap-2 rounded-xl bg-slate-50 p-2 pl-3">
              <span className="min-w-0 flex-1 truncate text-sm text-gray-600" title={enlace}>
                {enlace}
              </span>
              <button
                type="button"
                onClick={copiar}
                className="flex h-9 shrink-0 items-center gap-1.5 rounded-lg bg-white px-3 text-sm font-semibold text-violet-700 shadow-sm hover:bg-violet-50"
              >
                {copiado ? <Check className="size-4" aria-hidden="true" /> : <Copy className="size-4" aria-hidden="true" />}
                {copiado ? 'Copiado' : 'Copiar'}
              </button>
            </div>
            <p className="sr-only" aria-live="polite">{copiado ? 'Enlace copiado' : ''}</p>

            {error && (
              <p role="alert" className="mt-3 text-sm text-red-700">{error}</p>
            )}

            {puedeCompartirNativo && (
              <button
                type="button"
                onClick={compartirNativo}
                className="mt-4 flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-violet-600 font-bold text-white hover:bg-violet-700"
              >
                <Share2 className="size-5" aria-hidden="true" />
                Más opciones
              </button>
            )}

            <p className="mt-4 text-center text-xs text-gray-500">
              Quien abra el enlace solo verá esta meta y tu nombre con la inicial del apellido.
            </p>
          </>
        )}
      </section>
    </div>
  );
}
