import { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';
import MensajeError from './MensajeError';

export default function CampoTexto({
  id, etiqueta, tipo = 'text', valor, onChange, placeholder, error,
  deshabilitado = false, autoComplete, maxLength,
}) {
  const [visible, setVisible] = useState(false);
  const esContrasena = tipo === 'password';

  const borde = error ? 'border-red-400' : 'border-zinc-200';
  const fondo = deshabilitado ? 'bg-slate-100 text-gray-500' : 'bg-white text-gray-900';

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-bold text-gray-900">{etiqueta} *</label>
      <div className="relative">
        <input
          id={id}
          type={esContrasena && visible ? 'text' : tipo}
          value={valor}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          disabled={deshabilitado}
          autoComplete={autoComplete}
          maxLength={maxLength}
          aria-required="true"
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `error-${id}` : undefined}
          className={`h-[46px] w-full rounded-xl border px-4 text-[15px] outline-none placeholder:text-gray-400 focus:ring-2 focus:ring-violet-300 ${borde} ${fondo} ${esContrasena ? 'pr-12' : ''}`}
        />
        {esContrasena && (
          <button
            type="button"
            onClick={() => setVisible((v) => !v)}
            disabled={deshabilitado}
            aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            aria-pressed={visible}
            className="absolute inset-y-0 right-1 flex w-11 items-center justify-center text-gray-400 hover:text-gray-600 disabled:cursor-not-allowed"
          >
            {visible
              ? <EyeOff className="size-[18px]" aria-hidden="true" />
              : <Eye className="size-[18px]" aria-hidden="true" />}
          </button>
        )}
      </div>
      <MensajeError id={`error-${id}`} texto={error} />
    </div>
  );
}
