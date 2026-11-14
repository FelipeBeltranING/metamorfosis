import { useEffect, useState } from 'react';
import { ArrowRight, MailOpen } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { listarInvitaciones } from '../services/invitacionService';

/** HU14: aviso en Mis metas cuando hay invitaciones pendientes. Si falla la consulta, no muestra nada. */
export default function AvisoInvitaciones() {
  const { t } = useTranslation();
  const [cantidad, setCantidad] = useState(0);

  useEffect(() => {
    let activo = true;
    listarInvitaciones().then((lista) => activo && setCantidad(lista.length)).catch(() => {});
    return () => { activo = false; };
  }, []);

  if (!cantidad) return null;
  return (
    <Link
      to="/invitaciones"
      className="mb-5 flex items-center gap-3 rounded-2xl border border-violet-200 bg-violet-50 p-4 text-[15px] font-semibold text-violet-800 hover:bg-violet-100"
    >
      <MailOpen className="size-5 shrink-0" aria-hidden="true" />
      <span className="flex-1">
        {t('avisoInvitaciones.texto', { count: cantidad })}
      </span>
      <span className="inline-flex items-center gap-1 font-bold">{t('comun.ver')} <ArrowRight className="size-4" aria-hidden="true" /></span>
    </Link>
  );
}
