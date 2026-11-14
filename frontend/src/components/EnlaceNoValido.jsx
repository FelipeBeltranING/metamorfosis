import { Link2Off } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

/** HU8: el enlace del correo venció, ya se usó o no existe. */
export default function EnlaceNoValido({ mensaje }) {
  const { t } = useTranslation();
  return (
    <div className="flex flex-col items-center text-center">
      <div className="flex size-16 items-center justify-center rounded-2xl bg-red-50 text-red-600">
        <Link2Off className="size-8" aria-hidden="true" />
      </div>
      <p className="mt-4 text-[15px] leading-6 text-gray-600">
        {mensaje || t('enlaceNoValido.texto')}
      </p>
      <Link
        to="/registro"
        className="mt-6 flex h-[46px] w-full items-center justify-center rounded-xl bg-violet-600 font-bold text-white hover:bg-violet-700"
      >
        {t('enlaceNoValido.boton')}
      </Link>
    </div>
  );
}
