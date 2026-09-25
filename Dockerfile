FROM eclipse-temurin:17-jdk-jammy

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       xvfb \
       x11vnc \
       openbox \
       novnc \
       websockify \
       fonts-dejavu-core \
       ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .

RUN chmod +x build.sh docker/start-render.sh \
    && ./build.sh \
    && cp docker/index.html /usr/share/novnc/index.html

ENV DISPLAY=:99
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -Djava.awt.headless=false"

EXPOSE 10000

CMD ["./docker/start-render.sh"]
