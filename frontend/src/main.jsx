import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './i18n'
// RQ5: complemento de accesibilidad (Sienna, MIT). Se inicia solo y agrega un botón flotante con
// tamaño de letra, contraste, fuente para dislexia, guía de lectura, pausar animaciones y cursor grande.
// Sigue el atributo lang del <html>, así que cambia de idioma junto con la app (i18n).
import 'sienna-accessibility'
import App from './App.jsx'
import { aplicarApariencia, leerApariencia, seguirAlDispositivo } from './tema/apariencia'

// HU19: el tema se aplica antes de pintar para que no parpadee el violeta.
aplicarApariencia(leerApariencia())
seguirAlDispositivo()

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)