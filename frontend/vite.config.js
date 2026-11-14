import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import JavaScriptObfuscator from 'javascript-obfuscator'

/**
 * RQ6 (Entrega final): ofusca el JavaScript del cliente al construir para producción.
 * Corre sobre cada chunk ya minificado, así que no afecta `npm run dev`.
 * Se dejan apagadas las opciones agresivas (controlFlowFlattening, deadCodeInjection,
 * selfDefending) porque aumentan mucho el tamaño y el tiempo de carga.
 */
function ofuscarCliente() {
  return {
    name: 'metamorfosis-ofuscar-cliente',
    apply: 'build',
    renderChunk(codigo) {
      const resultado = JavaScriptObfuscator.obfuscate(codigo, {
        compact: true,
        identifierNamesGenerator: 'hexadecimal',
        renameGlobals: false,
        stringArray: true,
        stringArrayEncoding: ['base64'],
        stringArrayThreshold: 0.75,
        controlFlowFlattening: false,
        deadCodeInjection: false,
        selfDefending: false,
        sourceMap: false,
      })
      return { code: resultado.getObfuscatedCode(), map: null }
    },
  }
}

export default defineConfig({
  plugins: [react(), tailwindcss(), ofuscarCliente()],
  build: {
    // RQ6: minificación activa y sin source maps para no publicar el código original.
    minify: true,
    sourcemap: false,
  },
})
