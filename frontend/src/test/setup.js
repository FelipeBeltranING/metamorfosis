import '@testing-library/jest-dom/vitest';
// Las pruebas corren en español (el idioma por defecto de la app), sin importar el idioma del equipo.
import i18n from '../i18n';

await i18n.changeLanguage('es');
