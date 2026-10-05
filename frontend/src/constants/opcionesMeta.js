import {
  User, Briefcase, Wallet, Users, HeartPulse, GraduationCap, Palette, Ellipsis,
} from 'lucide-react';

export const NOMBRE_MAX = 200;

export const PLAZOS = [
  { valor: 'CORTO', etiqueta: 'Corto plazo' },
  { valor: 'MEDIANO', etiqueta: 'Mediano plazo' },
  { valor: 'LARGO', etiqueta: 'Largo plazo' },
];

export const TIPOS = [
  { valor: 'PERSONAL', etiqueta: 'Personal', Icono: User },
  { valor: 'PROFESIONAL', etiqueta: 'Profesional', Icono: Briefcase },
  { valor: 'FINANCIERA', etiqueta: 'Financiera', Icono: Wallet },
  { valor: 'SOCIAL_FAMILIAR', etiqueta: 'Social / Familiar', Icono: Users },
  { valor: 'SALUD_BIENESTAR', etiqueta: 'Salud y bienestar', Icono: HeartPulse },
  { valor: 'ACADEMICA', etiqueta: 'Académica', Icono: GraduationCap },
  { valor: 'RECREATIVA_OCIO', etiqueta: 'Recreativa / Ocio', Icono: Palette },
  { valor: 'OTRO', etiqueta: 'Otro', Icono: Ellipsis },
];