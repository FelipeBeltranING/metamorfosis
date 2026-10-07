import { Target } from 'lucide-react';
import { Link } from 'react-router-dom';

export default function EstadoVacio() {
  return (
    <div className="flex flex-col items-center gap-6 px-4 py-16 text-center sm:py-24">
      <div className="flex size-24 items-center justify-center rounded-3xl bg-violet-50">
        <Target className="size-12 text-violet-600" aria-hidden="true" />
      </div>
      <div className="flex flex-col gap-2">
        <h2 className="text-2xl font-bold text-gray-900">Aún no tienes metas activas</h2>
        <p className="text-base text-gray-500">
          Cada gran cambio empieza con un primer paso. Crea tu primera meta y empieza hoy.
        </p>
      </div>
      <Link
        to="/metas/nueva"
        className="flex h-14 items-center justify-center rounded-xl bg-violet-600 px-6 text-base font-bold text-white hover:bg-violet-700"
      >
        Crear mi primera meta
      </Link>
    </div>
  );
}