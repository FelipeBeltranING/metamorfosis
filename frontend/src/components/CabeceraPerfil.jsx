import { CalendarDays, Mail, Quote } from 'lucide-react';
import { PORTADA_POR_VALOR } from '../constants/portadas';
import { iniciales, mesYAnio } from '../utils/perfil';

/**
 * Cabecera del perfil: portada (HU19), foto o iniciales, nombre, frase (HU19), correo (solo en
 * Mi perfil) y fecha de registro. `acciones` va a la derecha (p. ej. el botón Editar perfil).
 * Se usa en Mi perfil, en el perfil de otra persona (HU24) y en la vista previa de Editar perfil.
 */
export default function CabeceraPerfil({
  nombre, apellido, foto, email, fechaRegistro, frase, portada, acciones, extraNombre, compacta = false,
}) {
  const estiloPortada = PORTADA_POR_VALOR[portada]?.estilo ?? null;
  const nombreCompleto = [nombre, apellido].filter(Boolean).join(' ');
  const tamano = compacta ? 'size-16 text-2xl' : 'size-16 text-2xl sm:size-20 sm:text-3xl';

  return (
    <section aria-label="Datos personales" className="overflow-hidden rounded-2xl border border-zinc-200 bg-white">
      {estiloPortada && <div className={compacta ? 'h-20' : 'h-24 sm:h-32'} style={estiloPortada} aria-hidden="true" />}
      <div className={`flex flex-col items-center gap-4 p-5 text-center ${compacta ? '' : 'sm:flex-row sm:items-end sm:p-6 sm:text-left'}`}>
        <div className={`shrink-0 rounded-full ${estiloPortada ? `-mt-14 ring-4 ring-[color:var(--tarjeta,#fff)] ${compacta ? '' : 'sm:-mt-16 sm:self-start'}` : ''}`}>
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
              {`Miembro desde ${mesYAnio(fechaRegistro)}`}
            </p>
          )}
        </div>
        {acciones}
      </div>
    </section>
  );
}
