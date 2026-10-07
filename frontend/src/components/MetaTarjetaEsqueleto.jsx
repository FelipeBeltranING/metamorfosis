export default function MetaTarjetaEsqueleto() {
  return (
    <li
      aria-hidden="true"
      className="flex animate-pulse flex-col gap-5 rounded-2xl border border-zinc-200 bg-white p-5 sm:p-8"
    >
      <div className="flex items-center justify-between">
        <div className="size-12 rounded-xl bg-slate-200" />
        <div className="h-9 w-20 rounded-xl bg-slate-200" />
      </div>
      <div className="flex flex-col gap-2">
        <div className="h-6 rounded-md bg-slate-200" />
        <div className="h-6 w-60 max-w-full rounded-md bg-slate-200" />
      </div>
      <div className="flex gap-2">
        <div className="h-9 w-28 rounded-xl bg-slate-200" />
        <div className="h-9 w-32 rounded-xl bg-slate-200" />
      </div>
    </li>
  );
}