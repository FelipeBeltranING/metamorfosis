import { useEffect, useRef, useState } from 'react';
import { LoaderCircle, LogOut, X } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { cerrarSesionRemota } from '../services/authService';
import { cerrarSesion, obtenerSesion } from '../services/sesionService';

/**
 * Botón "Cerrar sesión" del encabezado (donde estaba "Mis logros").
 * Pide confirmación antes de salir, porque ahora está a un toque.
 * En el celular solo se ve el ícono.
 */
export default function BotonCerrarSesion() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [confirmando, setConfirmando] = useState(false);
  const [cerrando, setCerrando] = useState(false);
  const boton = useRef(null);
  const cancelar = useRef(null);

  // Al abrir la confirmación el foco va a "Cancelar"; Esc la cierra y devuelve el foco al botón.
  useEffect(() => {
    if (!confirmando) return undefined;
    cancelar.current?.focus();
    const alPresionarTecla = (evento) => {
      if (evento.key === 'Escape' && !cerrando) {
        setConfirmando(false);
        boton.current?.focus();
      }
    };
    document.addEventListener('keydown', alPresionarTecla);
    return () => document.removeEventListener('keydown', alPresionarTecla);
  }, [confirmando, cerrando]);

  const cerrarConfirmacion = () => {
    if (cerrando) return;
    setConfirmando(false);
    boton.current?.focus();
  };

  const cerrarLaSesion = async () => {
    setCerrando(true);
    const sesion = obtenerSesion();
    try {
      if (sesion) await cerrarSesionRemota(sesion.token);
    } catch {
      // Si el servidor no responde (o el token ya no servía), la sesión se cierra igual aquí.
    }
    cerrarSesion();
    navigate('/login', { replace: true, state: { sesionCerrada: true } });
  };

  return (
    <>
      <button
        ref={boton}
        type="button"
        onClick={() => setConfirmando(true)}
        aria-label={t('auth.cerrarSesion.boton')}
        className="flex items-center gap-2 rounded-lg px-2.5 py-2 text-sm font-semibold text-gray-600 hover:bg-gray-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-violet-300 sm:px-3 sm:text-base"
      >
        <LogOut className="size-5" aria-hidden="true" />
        <span className="hidden sm:inline">{t('auth.cerrarSesion.boton')}</span>
      </button>

      {confirmando && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
          onMouseDown={(e) => {
            if (e.target === e.currentTarget) cerrarConfirmacion();
          }}
        >
          <section
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="titulo-cerrar-sesion"
            aria-describedby="descripcion-cerrar-sesion"
            className="w-full max-w-md rounded-2xl bg-white p-6 text-left shadow-2xl"
          >
            <div className="mb-5 flex items-start justify-between">
              <div className="flex size-12 items-center justify-center rounded-xl bg-violet-50 text-violet-600">
                <LogOut className="size-6" aria-hidden="true" />
              </div>
              <button
                type="button"
                onClick={cerrarConfirmacion}
                disabled={cerrando}
                aria-label={t('auth.cerrarSesion.cerrarVentana')}
                className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 disabled:opacity-50"
              >
                <X className="size-5" aria-hidden="true" />
              </button>
            </div>
            <h2 id="titulo-cerrar-sesion" className="text-xl font-bold text-gray-900">{t('auth.cerrarSesion.titulo')}</h2>
            <p id="descripcion-cerrar-sesion" className="mt-3 text-sm leading-6 text-gray-600">
              {t('auth.cerrarSesion.texto')}
            </p>
            <div className="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              <button
                ref={cancelar}
                type="button"
                onClick={cerrarConfirmacion}
                disabled={cerrando}
                className="h-11 rounded-xl border border-gray-200 px-5 font-semibold text-gray-700 hover:bg-gray-50 disabled:opacity-50"
              >
                {t('comun.cancelar')}
              </button>
              <button
                type="button"
                onClick={cerrarLaSesion}
                disabled={cerrando}
                className="flex h-11 items-center justify-center gap-2 rounded-xl bg-violet-600 px-5 font-semibold text-white hover:bg-violet-700 disabled:opacity-60"
              >
                {cerrando && <LoaderCircle className="size-5 animate-spin" aria-hidden="true" />}
                {cerrando ? t('auth.cerrarSesion.cerrando') : t('auth.cerrarSesion.boton')}
              </button>
            </div>
          </section>
        </div>
      )}
    </>
  );
}
