import { Sprout } from 'lucide-react';

export default function EncabezadoAuth() {
  return (
    <header className="flex h-16 items-center border-b border-zinc-200 bg-white px-4 sm:px-14">
      <div className="flex items-center gap-2.5">
        <div className="flex size-8 items-center justify-center rounded-lg bg-violet-600">
          <Sprout className="size-5 text-white" aria-hidden="true" />
        </div>
        <span className="text-lg font-bold text-gray-900">Metamorfosis</span>
      </div>
    </header>
  );
}
