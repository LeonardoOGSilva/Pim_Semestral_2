@echo off
title BoxTech - Iniciar
set "RAIZ=%~dp0"
echo Pasta do projeto: %RAIZ%
if not exist "%RAIZ%BoxTechFront\Imagens" (
  echo ERRO: nao achei BoxTechFront\Imagens. Coloque este arquivo na pasta que contem BoxTechAPI e BoxTechFront.
  pause
  exit /b 1
)
start "BoxTech API" /d "%RAIZ%BoxTechAPI" cmd /k dotnet run --urls http://0.0.0.0:5132
start "BoxTech Site 5500" /d "%RAIZ%BoxTechFront" cmd /k python -m http.server 5500 --bind 0.0.0.0
start "BoxTech Imagens App 5501" /d "%RAIZ%BoxTechFront" cmd /k python -m http.server 5501 --bind 0.0.0.0
echo.
echo IP do notebook (use no app, em Configurar servidor):
ipconfig | findstr /i "IPv4"
timeout /t 8 >nul
start http://localhost:5500
echo.
echo Teste de imagem no celular: http://SEU-IP:5501/Imagens/SSD1TB.png
echo Para encerrar, feche as tres janelas pretas.
pause
