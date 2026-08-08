# Multi-stage Dockerfile for ProcureAI Spring Boot (Java 17) from root workspace

# Stage 1: Build JAR artifact
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY BACKEND/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY BACKEND/src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Production JRE Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root app user & output directory for PDF generation
RUN addgroup -S procuregroup && adduser -S procureuser -G procuregroup \
    && mkdir -p /app/po-output && chown -R procureuser:procuregroup /app

COPY --from=builder /app/target/*.jar app.jar
RUN chown procureuser:procuregroup /app/app.jar

USER procureuser

EXPOSE 8080
ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
