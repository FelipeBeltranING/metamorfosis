import { CircleCheck, Pin, Target } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { TIPOS } from '../constants/opcionesMeta';
import { formatearFecha } from '../utils/fechas';

const TIPO_POR_VALOR = Object.fromEntries(TIPOS.map((x) => [x.valor, x]));

/**
 * HU19: las metas que la persona fijó en su perfil (máximo tres). En Mi perfil abren el camino;
 * en el perfil de otra persona no son enlaces (su camino no se puede abrir).
 */
export default function MetasDestacadas({ metas, publico = false, compacta = false }) {
  const { t } = useTranslation();
  if (!metas?.length) return null;
  return (
    <section aria-labelledby="perfil-destacadas" className={compacta ? '' : 'rounded-2xl border border-zinc-200 bg-white p-5 sm:p-6'}>
      <h2 id="perfil-destacadas" className={`flex items-center gap-2 font-bold text-gray-900 ${compacta ? 'mb-2 text-sm' : 'mb-4 text-lg'}`}>
        <Pin className={compacta ? 'size-4 text-violet-600' : 'size-5 text-violet-600'} aria-hidden="true" />
        {t('perfil.destacadas')}
      </h2>
      <ul className={`grid gap-3 ${compacta ? '' : 'sm:grid-cols-3'}`}>
        {metas.map((meta) => {
          const tipo = TIPO_POR_VALOR[meta.tipo] ?? TIPO_POR_VALOR.OTRO;
          const cumplida = meta.estado === 'CUMPLIDA';
          const nombre = publico || compacta
            ? <p className="break-words text-[15px] font-bold leading-snug text-gray-900">{meta.nombre}</p>
            : (
              <Link to={`/metas/${meta.codigoMeta}`} className="break-words rounded-md text-[15px] font-bold leading-snug text-gray-900 hover:text-violet-700">
                {meta.nombre}
              </Link>
            );
          return (
            <li key={meta.codigoMeta} className={`flex gap-3 rounded-xl border border-violet-100 bg-violet-50/60 p-4 ${compacta ? 'items-center' : 'flex-col'}`}>
              <div className={`flex size-10 shrink-0 items-center justify-center rounded-xl ${cumplida ? 'bg-emerald-100' : 'bg-white'}`}>
                <tipo.Icono className={`size-5 ${cumplida ? 'text-emerald-600' : 'text-violet-600'}`} aria-hidden="true" />
              </div>
              <div className="min-w-0 flex-1">
                {nombre}
                <p className={`mt-1 flex items-center gap-1.5 text-[13px] font-semibold ${cumplida ? 'text-emerald-700' : 'text-violet-700'}`}>
                  {cumplida ? <CircleCheck className="size-3.5" aria-hidden="true" /> : <Target className="size-3.5" aria-hidden="true" />}
                  {cumplida ? t('perfil.cumplidaEl', { fecha: formatearFecha(meta.fechaCumplida) }) : t('perfil.enCurso', { tipo: tipo.etiqueta })}
                </p>
              </div>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
