# Multi-stage build from monorepo root (Render dockerContext: .)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /src
COPY . .
RUN chmod +x mvnw \
  && ./mvnw -pl services/match-service -am package -Dmaven.test.skip=true -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /src/services/match-service/target/*.jar app.jar
RUN chown spring:spring app.jar
USER spring:spring
EXPOSE 10000
ENV JAVA_OPTS="-Xms256m -Xmx512m"
# Render injects PORT; fall back to the EXPOSEd port for plain docker run.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-10000} -jar app.jar"]
