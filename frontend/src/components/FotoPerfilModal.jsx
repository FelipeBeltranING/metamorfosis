import { useEffect, useRef, useState } from 'react';
import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';

/** Lado del recorte en pantalla y de la imagen que se guarda (px). */
const VISOR = 240;
const SALIDA = 320;
const ZOOM_MAX = 3;
const PASO_TECLA = 10;

/** El desplazamiento no deja ver bordes vacíos: la imagen siempre cubre el círculo. */
function limitar(desplazamiento, imagen, zoom) {
  if (!imagen) return desplazamiento;
  const escala = Math.max(VISOR / imagen.naturalWidth, VISOR / imagen.naturalHeight) * zoom;
  const maxX = (imagen.naturalWidth * escala - VISOR) / 2;
  const maxY = (imagen.naturalHeight * escala - VISOR) / 2;
  return {
    x: Math.min(maxX, Math.max(-maxX, desplazamiento.x)),
    y: Math.min(maxY, Math.max(-maxY, desplazamiento.y)),
  };
}

/** Estilo de la imagen dentro de un círculo de `lado` px (el visor o las vistas pequeñas). */
function estiloImagen(imagen, zoom, desplazamiento, lado) {
  if (!imagen) return { display: 'none' };
  const factor = lado / VISOR;
  const escala = Math.max(VISOR / imagen.naturalWidth, VISOR / imagen.naturalHeight) * zoom * factor;
  const ancho = imagen.naturalWidth * escala;
  const alto = imagen.naturalHeight * escala;
  return {
    position: 'absolute',
    width: ancho,
    height: alto,
    maxWidth: 'none',
    left: (lado - ancho) / 2 + desplazamiento.x * factor,
    top: (lado - alto) / 2 + desplazamiento.y * factor,
  };
}

/**
 * HU-23: vista previa de la foto elegida. Se arrastra (o se mueve con las flechas) para
 * centrarla y se acerca con el control. "Usar esta foto" entrega la imagen recortada en
 * un JPEG cuadrado de 320 px (unos KB) como data URL.
 */
export default function FotoPerfilModal({ archivo, onUsar, onElegirOtra, onCancelar, onError }) {
  const { t } = useTranslation();
  const [url, setUrl] = useState(null);
  const [imagen, setImagen] = useState(null);
  const [zoom, setZoom] = useState(1);
  const [desplazamiento, setDesplazamiento] = useState({ x: 0, y: 0 });
  const arrastre = useRef(null);
  const cerrar = useRef(null);

  // Se lee el archivo como data URL (no una dirección temporal que haya que liberar: en modo
  // desarrollo React monta el componente dos veces y una dirección liberada ya no abre).
  useEffect(() => {
    const lector = new FileReader();
    lector.onload = () => setUrl(lector.result);
    lector.onerror = () => onError(t('fotoModal.noAbre', { archivo: archivo.name }));
    lector.readAsDataURL(archivo);
    return () => {
      lector.onload = null;
      lector.onerror = null;
      lector.abort();
    };
  }, [archivo, onError, t]);

  useEffect(() => {
    cerrar.current?.focus();
    const alPresionarTecla = (evento) => {
      if (evento.key === 'Escape') onCancelar();
    };
    document.addEventListener('keydown', alPresionarTecla);
    return () => document.removeEventListener('keydown', alPresionarTecla);
  }, [onCancelar]);

  const mover = (dx, dy) => setDesplazamiento((actual) => limitar({ x: actual.x + dx, y: actual.y + dy }, imagen, zoom));

  const cambiarZoom = (valor) => {
    setZoom(valor);
    setDesplazamiento((actual) => limitar(actual, imagen, valor));
  };

  const alPresionarEnVisor = (evento) => {
    evento.currentTarget.setPointerCapture(evento.pointerId);
    arrastre.current = { x: evento.clientX, y: evento.clientY };
  };
  const alMoverEnVisor = (evento) => {
    if (!arrastre.current) return;
    mover(evento.clientX - arrastre.current.x, evento.clientY - arrastre.current.y);
    arrastre.current = { x: evento.clientX, y: evento.clientY };
  };
  const alSoltarEnVisor = () => {
    arrastre.current = null;
  };

  const alTeclaEnVisor = (evento) => {
    const pasos = {
      ArrowLeft: [-PASO_TECLA, 0], ArrowRight: [PASO_TECLA, 0], ArrowUp: [0, -PASO_TECLA], ArrowDown: [0, PASO_TECLA],
    };
    if (!pasos[evento.key]) return;
    evento.preventDefault();
    mover(...pasos[evento.key]);
  };

  const usar = () => {
    const escala = Math.max(VISOR / imagen.naturalWidth, VISOR / imagen.naturalHeight) * zoom;
    const izquierda = (VISOR - imagen.naturalWidth * escala) / 2 + desplazamiento.x;
    const arriba = (VISOR - imagen.naturalHeight * escala) / 2 + desplazamiento.y;
    const lienzo = document.createElement('canvas');
    lienzo.width = SALIDA;
    lienzo.height = SALIDA;
    const contexto = lienzo.getContext('2d');
    contexto.fillStyle = '#ffffff'; // fondo para PNG con transparencia
    contexto.fillRect(0, 0, SALIDA, SALIDA);
    contexto.imageSmoothingQuality = 'high';
    contexto.drawImage(imagen, -izquierda / escala, -arriba / escala, VISOR / escala, VISOR / escala, 0, 0, SALIDA, SALIDA);
    onUsar(lienzo.toDataURL('image/jpeg', 0.85));
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      role="presentation"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onCancelar();
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="titulo-foto"
        aria-describedby="descripcion-foto"
        className="flex max-h-full w-full max-w-[520px] flex-col gap-5 overflow-y-auto rounded-2xl bg-white p-5 shadow-2xl sm:p-7"
      >
        <div className="flex items-start justify-between gap-3">
          <div>
            <h2 id="titulo-foto" className="text-[22px] font-extrabold text-gray-900">{t('fotoModal.titulo')}</h2>
            <p id="descripcion-foto" className="mt-1 text-[15px] text-gray-600">{t('fotoModal.descripcion')}</p>
          </div>
          <button
            ref={cerrar}
            type="button"
            onClick={onCancelar}
            aria-label={t('fotoModal.cerrarVentana')}
            className="rounded-lg p-2 text-gray-500 hover:bg-gray-100"
          >
            <X className="size-5" aria-hidden="true" />
          </button>
        </div>

        <div className="flex justify-center rounded-2xl bg-[#1e1b2e] py-6">
          {/* RQ5: el visor se mueve con el puntero o con las flechas en dos ejes; no existe un rol ARIA
              de control 2D, así que se usa role="application" y jsx-a11y no lo reconoce como interactivo. */}
          {/* eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions, jsx-a11y/no-noninteractive-tabindex */}
          <div tabIndex={0}
            role="application"
            aria-roledescription={t('fotoModal.vistaPrevia')}
            aria-label={t('fotoModal.vistaPrevia')}
            onPointerDown={alPresionarEnVisor}
            onPointerMove={alMoverEnVisor}
            onPointerUp={alSoltarEnVisor}
            onPointerCancel={alSoltarEnVisor}
            onKeyDown={alTeclaEnVisor}
            className="relative cursor-grab touch-none select-none overflow-hidden rounded-full bg-violet-200 ring-4 ring-white focus-visible:outline-none focus-visible:ring-violet-300 active:cursor-grabbing"
            style={{ width: VISOR, height: VISOR }}
          >
            {url && (
              <img
                src={url}
                alt=""
                draggable={false}
                onLoad={(e) => setImagen(e.currentTarget)}
                onError={() => onError(t('fotoModal.noAbre', { archivo: archivo.name }))}
                style={estiloImagen(imagen, zoom, desplazamiento, VISOR)}
              />
            )}
          </div>
        </div>

        <div className="flex flex-col gap-1.5">
          <label htmlFor="zoom-foto" className="text-sm font-bold text-gray-700">{t('fotoModal.acercar')}</label>
          <input
            id="zoom-foto"
            type="range"
            min="1"
            max={ZOOM_MAX}
            step="0.05"
            value={zoom}
            onChange={(e) => cambiarZoom(Number(e.target.value))}
            disabled={!imagen}
            className="w-full accent-violet-600"
          />
        </div>

        <div className="flex items-center gap-3.5 rounded-xl bg-slate-50 px-3.5 py-3">
          {[48, 32].map((lado) => (
            <span key={lado} aria-hidden="true" className="relative shrink-0 overflow-hidden rounded-full bg-violet-200" style={{ width: lado, height: lado }}>
              {url && <img src={url} alt="" style={estiloImagen(imagen, zoom, desplazamiento, lado)} />}
            </span>
          ))}
          <p className="text-sm text-gray-600">{t('fotoModal.asiSeVera')}</p>
        </div>

        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={onElegirOtra}
            className="h-12 rounded-xl border border-zinc-200 bg-white px-5 font-bold text-gray-700 hover:bg-gray-50"
          >
            {t('fotoModal.elegirOtra')}
          </button>
          <button
            type="button"
            onClick={usar}
            disabled={!imagen}
            className="h-12 rounded-xl bg-violet-600 px-6 font-bold text-white hover:bg-violet-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {t('fotoModal.usar')}
          </button>
        </div>
      </section>
    </div>
  );
}
