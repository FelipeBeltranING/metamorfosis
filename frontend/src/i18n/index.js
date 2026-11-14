// Internacionalización: español (por defecto) e inglés con i18next.
// El idioma elegido se guarda en este navegador y, si no hay, se toma el del navegador.
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
// Cada archivo agrupa los textos de una parte de la app (comun, auth, metas...).
const archivosEs = import.meta.glob('./es/*.json', { eager: true, import: 'default' });
const archivosEn = import.meta.glob('./en/*.json', { eager: true, import: 'default' });
const unir = (archivos) => Object.assign({}, ...Object.values(archivos));
const es = unir(archivosEs);
const en = unir(archivosEn);

export const CLAVE_IDIOMA = 'metamorfosis.idioma';

export const IDIOMAS = [
  { valor: 'es', nombre: 'Español', corto: 'ES', regional: 'es-CO' },
  { valor: 'en', nombre: 'English', corto: 'EN', regional: 'en-US' },
];

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: { es: { translation: es }, en: { translation: en } },
    fallbackLng: 'es',
    supportedLngs: IDIOMAS.map((i) => i.valor),
    nonExplicitSupportedLngs: true,
    load: 'languageOnly',
    interpolation: { escapeValue: false }, // React ya escapa el texto.
    detection: {
      order: ['localStorage', 'navigator', 'htmlTag'],
      lookupLocalStorage: CLAVE_IDIOMA,
      caches: ['localStorage'],
    },
  });

/** Idioma activo ('es' o 'en'). */
export function idiomaActual() {
  const base = (i18n.resolvedLanguage || i18n.language || 'es').slice(0, 2);
  return IDIOMAS.some((i) => i.valor === base) ? base : 'es';
}

/** Código regional para Intl (fechas y números): 'es-CO' o 'en-US'. */
export function localeActual() {
  return IDIOMAS.find((i) => i.valor === idiomaActual()).regional;
}

function alCambiarIdioma() {
  document.documentElement.lang = idiomaActual();
  document.title = i18n.t('comun.app');
}
alCambiarIdioma();
i18n.on('languageChanged', alCambiarIdioma);

export const t = (...args) => i18n.t(...args);

export default i18n;

/** Cabecera para que el servidor responda los mensajes en el idioma de la app. */
export function cabeceraIdioma() {
  return { 'Accept-Language': idiomaActual() };
}
