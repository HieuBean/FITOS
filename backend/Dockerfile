# ==============================================================================
# FITOS Backend - Production Dockerfile (Multi-stage Java 21)
# Task #140 - Person D (Layer 5: DevOps)
# ==============================================================================

# Stage 1: Build stage with Maven & Eclipse Temurin JDK 21
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Caching dependencies layer
COPY pom.xml .
RUN mvn -q dependency:go-offline -B

# Compile source code and package executable JAR
COPY src ./src
RUN mvn -q clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Minimal Production Runtime with Temurin JRE 21 Alpine
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Security: Create non-root system group and user
RUN addgroup -S spring && adduser -S spring -G spring

# Copy pre-built JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Set file ownership to non-root user
RUN chown -R spring:spring /app

USER spring:spring

# Expose Spring Boot HTTP port
EXPOSE 8080

# Environment and JVM optimization options
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Healthcheck to verify service availability
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/api/studios || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
