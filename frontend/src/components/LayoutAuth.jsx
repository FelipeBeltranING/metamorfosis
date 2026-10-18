import EncabezadoAuth from './EncabezadoAuth';

export default function LayoutAuth({ titulo, subtitulo, children }) {
  return (
    <div className="min-h-screen bg-slate-50 text-gray-900">
      <EncabezadoAuth />
      <main className="flex min-h-[calc(100vh-4rem)] items-start justify-center px-4 pb-24 pt-8 sm:items-center">
        <section className="w-full max-w-[440px] rounded-2xl border border-zinc-200 bg-white p-6 shadow-[0_4px_20px_rgba(30,27,46,0.03)] sm:p-8">
          <h1 className="text-2xl font-bold">{titulo}</h1>
          <p className="mt-1 text-sm text-gray-500">{subtitulo}</p>
          <div className="mt-5">{children}</div>
        </section>
      </main>
    </div>
  );
}