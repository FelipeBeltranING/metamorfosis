# Metamorfosis: Frontend

Interfaz web de Metamorfosis, hecha con React, Vite y Tailwind CSS.

## Requisitos
- Node.js 20 o superior
- Backend corriendo en `http://localhost:8080`

## Configuración
1. Instalar dependencias:
```bash
   npm install
```
2. Crear el archivo `.env.local` a partir de la plantilla:
```bash
   cp .env.example .env.local
```
   (En Windows PowerShell: `Copy-Item .env.example .env.local`)
3. Ajustar `VITE_API_URL` si el backend usa otra dirección.

## Ejecución
```bash
npm run dev      # servidor de desarrollo en http://localhost:5173
npm run build    # compilación para producción
npm run lint     # revisión de código con ESLint
```

## Estructura
```
src/
├── components/   componentes reutilizables
├── constants/    valores fijos (plazos, tipos de meta)
├── pages/        pantallas
└── services/     llamadas a la API
```