#!/usr/bin/env bash
set -euo pipefail

export DISPLAY="${DISPLAY:-:99}"
PORT="${PORT:-10000}"

mkdir -p /app/dados-academico
rm -f /tmp/.X99-lock || true

Xvfb "${DISPLAY}" -screen 0 1280x800x24 -ac -noreset &
sleep 1

openbox-session >/tmp/openbox.log 2>&1 &
sleep 1

x11vnc \
  -display "${DISPLAY}" \
  -rfbport 5900 \
  -forever \
  -shared \
  -nopw \
  -noxdamage \
  -quiet &
sleep 1

java -jar dist/GestaoToner-Academico.jar >/tmp/gestao-toner.log 2>&1 &
sleep 2

exec websockify --web=/usr/share/novnc "${PORT}" localhost:5900
