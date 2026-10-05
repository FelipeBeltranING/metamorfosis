import { CircleAlert } from 'lucide-react';

export default function MensajeError({ id, texto }) {
  if (!texto) return null;
  return (
    <p id={id} role="alert" className="flex items-center gap-1.5 text-sm font-semibold text-red-600">
      <CircleAlert className="size-4 shrink-0" aria-hidden="true" />
      {texto}
    </p>
  );
}