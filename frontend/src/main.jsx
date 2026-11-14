import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './i18n'
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