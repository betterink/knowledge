# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.gradle ./gradlew installDist --no-daemon

FROM eclipse-temurin:21-jre
RUN useradd --system --no-create-home --shell /usr/sbin/nologin app
COPY --from=build /src/build/install/knowledge-backend /opt/knowledge-backend
ENV VAULT_ROOT=/vault PORT=8080
USER app
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s CMD curl -fsS "http://127.0.0.1:${PORT}/health" || exit 1
CMD ["/opt/knowledge-backend/bin/knowledge-backend"]
