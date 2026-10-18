import { CircleAlert } from 'lucide-react';

export default function AlertaFormulario({ texto }) {
  if (!texto) return null;
  return (
    <p
      role="alert"
      className="mb-4 flex items-start gap-2 rounded-xl bg-red-50 px-3 py-3 text-sm font-bold text-red-600"
    >
      <CircleAlert className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
      {texto}
    </p>
  );
}
