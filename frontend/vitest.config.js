import { defineConfig } from 'vitest/config';

/**
 * RQ9 (evidencia de pruebas): pruebas unitarias del frontend con Vitest.
 * Va aparte de vite.config.js para no cargar en las pruebas el ofuscador ni Tailwind.
 * `npm test` las corre una vez; `npm run test:cobertura` deja el informe en coverage/index.html.
 */
export default defineConfig({
  // Mismo JSX que la app (React 19, sin importar React en cada archivo).
  esbuild: { jsx: 'automatic' },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.js'],
    include: ['src/**/*.test.{js,jsx}'],
    coverage: {
      provider: 'v8',
      // Se mide la lógica de src/utils. Los componentes probados (HU6 y HU27) no entran en el porcentaje
      // para no dar una cifra engañosa: el resto de pantallas se prueba a mano en el navegador.
      include: ['src/utils/**'],
      exclude: ['src/utils/tarjetaLogro.js'], // dibuja en un <canvas>: se prueba en el navegador (HU21)
      reporter: ['text', 'html'],
    },
  },
});
