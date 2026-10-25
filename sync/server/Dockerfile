# The Sumizuri sync server as a container, holding the server the release workflow built in a small Java runtime for x86 and ARM.
FROM eclipse-temurin:21-jre

LABEL org.opencontainers.image.title="Sumizuri sync server" \
      org.opencontainers.image.description="The sync server of the Sumizuri app. It keeps libraries locked, so it can not read them." \
      org.opencontainers.image.source="https://github.com/not-akari/sumizuri"

# The server runs as its own user, never as root, and keeps everything in /data.
RUN groupadd --system sumizuri \
    && useradd --system --gid sumizuri --home-dir /data --shell /usr/sbin/nologin sumizuri \
    && mkdir -p /data \
    && chown sumizuri:sumizuri /data

# The jars can come out of the Gradle cache readable only by their owner, so the copy makes them readable for the server user.
COPY --chmod=755 build/install/server /opt/sumizuri

ENV SUMIZURI_DATA_DIR=/data \
    SUMIZURI_HOST=0.0.0.0 \
    SUMIZURI_PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=50"

VOLUME /data
EXPOSE 8080
USER sumizuri
ENTRYPOINT ["/opt/sumizuri/bin/server"]
