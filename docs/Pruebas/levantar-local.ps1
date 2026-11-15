# Levanta Metamorfosis en este PC contra la base local de Docker (contenedor metamorfosis-pg-local).
# Abre dos ventanas: el backend en http://localhost:8080 y el frontend en http://localhost:5173.
# Uso, desde la raíz del repositorio:
#   powershell -ExecutionPolicy Bypass -File docs\Pruebas\levantar-local.ps1
# Solo para desarrollo: la clave del contenedor y el secreto JWT son de pruebas, no de producción.

$raiz = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

Write-Host 'Encendiendo la base de datos (Docker)...'
docker start metamorfosis-pg-local | Out-Null
if ($LASTEXITCODE -ne 0) { Write-Host 'No se pudo encender el contenedor. ¿Docker Desktop está abierto?'; exit 1 }

$backend = "`$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:55432/metamorfosis'; " +
  "`$env:SPRING_DATASOURCE_USERNAME='postgres'; `$env:SPRING_DATASOURCE_PASSWORD='local'; " +
  "`$env:APP_JWT_SECRET='secreto-de-pruebas-newman-32-bytes!!'; " +
  "`$env:APP_CORS_ALLOWED_ORIGINS='http://localhost:5173'; `$env:APP_FRONTEND_URL='http://localhost:5173'; " +
  "`$env:APP_CORREO_PROVEEDOR='consola'; " +
  "Set-Location '$raiz\backend'; .\mvnw.cmd spring-boot:run"
Start-Process powershell -ArgumentList '-NoExit', '-Command', $backend

$frontend = "`$env:VITE_API_URL='http://localhost:8080'; Set-Location '$raiz\frontend'; npm install; npm run dev"
Start-Process powershell -ArgumentList '-NoExit', '-Command', $frontend

Write-Host 'Listo: espera a que el backend diga "Started BackendApplication" y abre http://localhost:5173'
