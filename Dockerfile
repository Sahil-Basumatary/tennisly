# Render's Docker language defaults to ./Dockerfile at the repo root.
# TENNISLY_SERVICE / RENDER_SERVICE_NAME pick which Maven module to package.
# Do not ARG database passwords or tokens — Render forwards every env var as a build-arg.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
COPY . .
ARG TENNISLY_SERVICE
ARG RENDER_SERVICE_NAME
RUN MODULE="${TENNISLY_SERVICE:-${RENDER_SERVICE_NAME:-}}" \
  && if [ -z "$MODULE" ] || [ ! -f "services/${MODULE}/pom.xml" ]; then \
       echo "unknown TENNISLY_SERVICE/RENDER_SERVICE_NAME='$MODULE'" >&2; \
       ls services >&2; \
       exit 1; \
     fi \
  && chmod +x mvnw \
  && ./mvnw -pl "services/${MODULE}" -am package -Dmaven.test.skip=true -q \
  && cp services/"${MODULE}"/target/*.jar /src/app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /src/app.jar app.jar
RUN chown spring:spring app.jar
USER spring:spring
EXPOSE 10000
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-10000} -jar app.jar"]
