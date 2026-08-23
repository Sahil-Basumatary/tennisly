# Render's Docker language defaults to ./Dockerfile at the repo root.
# TENNISLY_SERVICE is the Maven folder under services/. RENDER_SERVICE_NAME is
# the dashboard slug (e.g. tennisly-8ggf) and is not a module name.
# Do not ARG database passwords or tokens — Render forwards every env var as a build-arg.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
COPY . .
ARG TENNISLY_SERVICE
ARG RENDER_SERVICE_NAME
ARG POSTGRES_DB_MATCHES
ARG POSTGRES_DB_TENNIS_DATA
ARG POSTGRES_DB_AUTH
ARG POSTGRES_DB_USERS
ARG POSTGRES_DB_NOTIFICATIONS
ARG CORS_ALLOWED_ORIGINS
ARG MATCH_INGEST_ENABLED
RUN set -eu; \
    MODULE="${TENNISLY_SERVICE:-}"; \
    if [ ! -f "services/${MODULE}/pom.xml" ]; then \
      MODULE=""; \
      for name in notification-service tennis-data-service analytics-service replay-service match-service auth-service user-service api-gateway; do \
        case "${RENDER_SERVICE_NAME:-}" in \
          "$name"|"$name"-*) MODULE="$name"; break ;; \
        esac; \
      done; \
    fi; \
    if [ ! -f "services/${MODULE}/pom.xml" ]; then \
      if [ -n "${POSTGRES_DB_MATCHES:-}" ] || [ -n "${MATCH_INGEST_ENABLED:-}" ]; then MODULE=match-service; \
      elif [ -n "${POSTGRES_DB_TENNIS_DATA:-}" ]; then MODULE=tennis-data-service; \
      elif [ -n "${POSTGRES_DB_AUTH:-}" ]; then MODULE=auth-service; \
      elif [ -n "${POSTGRES_DB_USERS:-}" ]; then MODULE=user-service; \
      elif [ -n "${POSTGRES_DB_NOTIFICATIONS:-}" ]; then MODULE=notification-service; \
      elif [ -n "${CORS_ALLOWED_ORIGINS:-}" ]; then MODULE=api-gateway; \
      fi; \
    fi; \
    if [ ! -f "services/${MODULE}/pom.xml" ]; then \
      echo "Set TENNISLY_SERVICE on this Render service to api-gateway, match-service, tennis-data-service, auth-service, user-service, or notification-service." >&2; \
      echo "RENDER_SERVICE_NAME='${RENDER_SERVICE_NAME:-}' is the Render slug, not a Maven module." >&2; \
      ls services >&2; \
      exit 1; \
    fi; \
    echo "packaging services/${MODULE}"; \
    chmod +x mvnw; \
    ./mvnw -pl "services/${MODULE}" -am package -Dmaven.test.skip=true -q; \
    cp services/"${MODULE}"/target/*.jar /src/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /src/app.jar app.jar
RUN chown spring:spring app.jar
USER spring:spring
EXPOSE 10000
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-10000} -jar app.jar"]
