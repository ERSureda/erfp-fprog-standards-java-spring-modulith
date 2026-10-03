# ETAPA 1: Compilación y empaquetado
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /workspace
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ETAPA 2: Runtime de producción seguro y ligero
FROM eclipse-temurin:25-jre-alpine AS runner
WORKDIR /app

# Creación de usuario sin privilegios administrativos (Principio de Mínimo Privilegio)
RUN addgroup -g 10001 -S nonroot && \
    adduser -u 10001 -S nonroot -G nonroot

COPY --from=builder /workspace/build/libs/*.jar app.jar
RUN chown -R nonroot:nonroot /app

USER nonroot:nonroot

EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
