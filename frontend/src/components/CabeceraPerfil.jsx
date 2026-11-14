import { CalendarDays, Mail, Quote } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { PORTADA_POR_VALOR } from '../constants/portadas';
import { avatarCompleto, pielOscura } from '../constants/avatar';
import { iniciales, mesYAnio } from '../utils/perfil';
import AvatarIlustrado from './AvatarIlustrado';

/**
 * Medidas del avatar asomado (HU20). La cabeza queda dentro de la portada y la barbilla pasa 14 px
 * por detrás del panel blanco; las manos se apoyan en el borde, como alguien asomado a una ventana.
 */
const ASOMADO = {
  normal: {
    banda: 'h-36 sm:h-44',
    cabeza: 'right-3 top-[30px] w-32 sm:right-40 sm:top-[22px] sm:w-[168px]',
    manos: 'right-3 top-[131px] w-32 sm:right-40 sm:top-[163px] sm:w-[168px]',
  },
  compacta: {
    banda: 'h-32',
    cabeza: 'right-4 top-[30px] w-28',
    manos: 'right-4 top-[115px] w-28',
  },
};

function Mano({ piel, className }) {
  const borde = pielOscura(piel);
  return (
    <svg viewBox="0 0 34 24" className={`absolute w-[20%] ${className}`} aria-hidden="true">
      <rect x="1" y="1" width="32" height="24" rx="11" fill={`#${piel}`} stroke={borde} strokeWidth="1.5" />
      <path d="M11 3v8M17 2v9M23 3v8" stroke={borde} strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

/**
 * Cabecera del perfil: portada (HU19), foto o iniciales, nombre, frase (HU19), avatar asomado (HU20),
 * correo (solo en Mi perfil) y fecha de registro. `acciones` va a la derecha (p. ej. el botón Editar
 * perfil). Se usa en Mi perfil, en el perfil de otra persona (HU24) y en la vista previa de Editar perfil.
 */
export default function CabeceraPerfil({
  nombre, apellido, foto, email, fechaRegistro, frase, portada, avatar, acciones, extraNombre, compacta = false,
}) {
  const { t } = useTranslation();
  const estiloPortada = PORTADA_POR_VALOR[portada]?.estilo ?? null;
  const nombreCompleto = [nombre, apellido].filter(Boolean).join(' ');
  const tamano = compacta ? 'size-16 text-2xl' : 'size-16 text-2xl sm:size-20 sm:text-3xl';
  const medidas = ASOMADO[compacta ? 'compacta' : 'normal'];
  const piezas = avatar ? avatarCompleto(avatar) : null;
  // Con avatar siempre hay banda arriba: la portada elegida o, si no eligió, un fondo suave.
  const hayBanda = Boolean(estiloPortada || piezas);
  const alturaBanda = piezas ? medidas.banda : compacta ? 'h-20' : 'h-24 sm:h-32';

  return (
    <section aria-label={t('perfil.datosPersonales')} className="relative overflow-hidden rounded-2xl border border-zinc-200 bg-white">
      {hayBanda && (
        <div className={`${alturaBanda} ${estiloPortada ? '' : 'bg-violet-100'}`} style={estiloPortada ?? undefined} aria-hidden="true" />
      )}
      {piezas && (
        <>
          <AvatarIlustrado
            avatar={piezas}
            volteado
            alt={t('avatar.alt', { nombre: nombreCompleto })}
            className={`absolute z-[1] ${medidas.cabeza}`}
          />
          <div className={`pointer-events-none absolute z-[3] aspect-[168/30] ${medidas.manos}`} aria-hidden="true">
            <Mano piel={piezas.piel} className="left-[7%]" />
            <Mano piel={piezas.piel} className="left-[54%]" />
          </div>
        </>
      )}
      <div className={`relative z-[2] flex flex-col items-center gap-4 bg-white p-5 text-center ${compacta ? '' : 'sm:flex-row sm:items-end sm:p-6 sm:text-left'}`}>
        <div className={`shrink-0 rounded-full ${hayBanda ? `-mt-14 ring-4 ring-[color:var(--tarjeta,#fff)] ${compacta ? '' : 'sm:-mt-16 sm:self-start'}` : ''}`}>
          {foto ? (
            <img src={foto} alt="" className={`${tamano} rounded-full object-cover`} />
          ) : (
            <div className={`${tamano} flex items-center justify-center rounded-full bg-violet-600 font-bold text-white`} aria-hidden="true">
              {iniciales(nombre, apellido)}
            </div>
          )}
        </div>
        <div className="flex min-w-0 flex-1 flex-col gap-1">
          <div className={`flex flex-wrap items-center justify-center gap-2 ${compacta ? '' : 'sm:justify-start'}`}>
            <p className="min-w-0 truncate text-xl font-bold text-gray-900 sm:text-2xl">{nombreCompleto}</p>
            {extraNombre}
          </div>
          {frase && (
            <p className={`flex items-start justify-center gap-1.5 text-[15px] italic leading-6 text-gray-700 ${compacta ? '' : 'sm:justify-start'}`}>
              <Quote className="mt-1 size-3.5 shrink-0 text-violet-400" aria-hidden="true" />
              <span className="break-words">{frase}</span>
            </p>
          )}
          {email && (
            <p className={`flex items-center justify-center gap-2 text-sm text-gray-600 ${compacta ? '' : 'sm:justify-start'}`}>
              <Mail className="size-4 shrink-0 text-gray-400" aria-hidden="true" />
              <span className="truncate">{email}</span>
            </p>
          )}
          {fechaRegistro && (
            <p className={`flex items-center justify-center gap-2 text-sm text-gray-600 ${compacta ? '' : 'sm:justify-start'}`}>
              <CalendarDays className="size-4 shrink-0 text-gray-400" aria-hidden="true" />
              {t('perfil.miembroDesde', { fecha: mesYAnio(fechaRegistro) })}
            </p>
          )}
        </div>
        {acciones}
      </div>
    </section>
  );
}
