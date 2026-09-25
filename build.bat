@echo off
setlocal
if not exist out mkdir out
if not exist dist mkdir dist
javac -encoding UTF-8 -d out src\GestaoTonerApp.java
if errorlevel 1 exit /b 1
jar cfm dist\GestaoToner-Academico.jar manifest.txt -C out .
echo.
echo Build concluido: dist\GestaoToner-Academico.jar
