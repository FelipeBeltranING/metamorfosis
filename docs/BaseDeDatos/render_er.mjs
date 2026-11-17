// Dibuja modelo_er.mmd como PNG con Mermaid en Chrome sin ventana.
// Uso: node render_er.mjs <modelo_er.mmd> <salida.png>   (necesita puppeteer-core y Google Chrome)
import { readFileSync } from 'node:fs';
import puppeteer from 'puppeteer-core';

const [entrada, salida] = process.argv.slice(2);
const codigo = readFileSync(entrada, 'utf8');
const navegador = await puppeteer.launch({
  executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe',
  headless: true,
  args: ['--no-sandbox'],
});
const pagina = await navegador.newPage();
await pagina.setViewport({ width: 2400, height: 1600, deviceScaleFactor: 2 });
await pagina.setContent(`<!doctype html><html><head><meta charset="utf-8">
<style>body{margin:0;background:#fff;font-family:Arial,sans-serif} #d{display:inline-block;padding:24px}</style>
<script src="https://cdn.jsdelivr.net/npm/mermaid@11.6.0/dist/mermaid.min.js"></script></head>
<body><div id="d"></div></body></html>`, { waitUntil: 'networkidle0' });
await pagina.evaluate(async (texto) => {
  mermaid.initialize({
    startOnLoad: false,
    theme: 'base',
    er: { layoutDirection: 'TB', minEntityWidth: 120, entityPadding: 12, fontSize: 14, useMaxWidth: false },
    themeVariables: {
      fontFamily: 'Arial, sans-serif',
      primaryColor: '#ede9fe', primaryBorderColor: '#7c3aed', primaryTextColor: '#1f1147',
      lineColor: '#6d28d9', textColor: '#1f1147',
      attributeBackgroundColorOdd: '#ffffff', attributeBackgroundColorEven: '#f5f3ff',
    },
  });
  const { svg } = await mermaid.render('er', texto);
  document.getElementById('d').innerHTML = svg;
}, codigo);
const caja = await (await pagina.$('#d')).boundingBox();
await pagina.setViewport({ width: Math.ceil(caja.width), height: Math.ceil(caja.height), deviceScaleFactor: 2 });
await (await pagina.$('#d')).screenshot({ path: salida });
await navegador.close();
console.log('ok', salida, Math.round(caja.width), 'x', Math.round(caja.height));
