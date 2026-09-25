#!/usr/bin/env sh
set -eu
mkdir -p out dist
javac -encoding UTF-8 -d out src/GestaoTonerApp.java
jar cfm dist/GestaoToner-Academico.jar manifest.txt -C out .
echo "Build concluído: dist/GestaoToner-Academico.jar"
