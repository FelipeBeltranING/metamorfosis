import { Sprout, User } from 'lucide-react';

export default function Encabezado() {
  return (
    <header className="flex h-16 items-center justify-between border-b border-zinc-200 bg-white px-4 sm:h-20 sm:px-12">
      <div className="flex items-center gap-3">
        <div className="flex size-10 items-center justify-center rounded-xl bg-violet-600">
          <Sprout className="size-6 text-white" aria-hidden="true" />
        </div>
        <span className="text-xl font-bold text-gray-900 sm:text-2xl">Metamorfosis</span>
      </div>
      <div className="flex size-10 items-center justify-center rounded-full bg-violet-100 sm:size-12">
        <User className="size-6 text-violet-600" aria-label="Perfil" />
      </div>
    </header>
  );
}