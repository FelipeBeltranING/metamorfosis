import {
  User, Briefcase, Wallet, Users, HeartPulse, GraduationCap, Palette, Ellipsis,
} from 'lucide-react';
import { t } from '../i18n';

export const NOMBRE_MAX = 200;

export const PLAZOS = [
  { valor: 'CORTO', get etiqueta() { return t('opciones.plazo.CORTO'); } },
  { valor: 'MEDIANO', get etiqueta() { return t('opciones.plazo.MEDIANO'); } },
  { valor: 'LARGO', get etiqueta() { return t('opciones.plazo.LARGO'); } },
];

export const TIPOS = [
  { valor: 'PERSONAL', get etiqueta() { return t('opciones.tipo.PERSONAL'); }, Icono: User },
  { valor: 'PROFESIONAL', get etiqueta() { return t('opciones.tipo.PROFESIONAL'); }, Icono: Briefcase },
  { valor: 'FINANCIERA', get etiqueta() { return t('opciones.tipo.FINANCIERA'); }, Icono: Wallet },
  { valor: 'SOCIAL_FAMILIAR', get etiqueta() { return t('opciones.tipo.SOCIAL_FAMILIAR'); }, Icono: Users },
  { valor: 'SALUD_BIENESTAR', get etiqueta() { return t('opciones.tipo.SALUD_BIENESTAR'); }, Icono: HeartPulse },
  { valor: 'ACADEMICA', get etiqueta() { return t('opciones.tipo.ACADEMICA'); }, Icono: GraduationCap },
  { valor: 'RECREATIVA_OCIO', get etiqueta() { return t('opciones.tipo.RECREATIVA_OCIO'); }, Icono: Palette },
  { valor: 'OTRO', get etiqueta() { return t('opciones.tipo.OTRO'); }, Icono: Ellipsis },
];