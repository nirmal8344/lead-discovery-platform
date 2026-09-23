# ==============================================================================
# Lead Discovery Platform - Production Dockerfile
# ==============================================================================
# Multi-stage build: Maven build → Playwright Java runtime
#
# Playwright approach: Uses the official Microsoft Playwright Java Docker image
# (mcr.microsoft.com/playwright/java) as the runtime base. This image ships with
# Chromium browser binaries AND all required system dependencies pre-installed.
# No Node.js/npm/npx is needed. This matches the Java Playwright dependency
# (com.microsoft.playwright:playwright:1.49.0) in pom.xml.
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build the Spring Boot fat JAR
# ------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy POM first for dependency layer caching
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw.cmd ./

# Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 2: Production runtime with Playwright Java + Chromium
# ------------------------------------------------------------------------------
# Official Playwright Java image - includes:
#   - Eclipse Temurin JDK 17
#   - Chromium browser binary (matching Playwright 1.49.0)
#   - All system dependencies (libnss3, libatk, libgbm, fonts, etc.)
#   - No Node.js/npm required
FROM mcr.microsoft.com/playwright/java:v1.49.0-noble AS runtime

WORKDIR /app

# Create non-root user for security
RUN groupadd --system appgroup && \
    useradd --system --gid appgroup --create-home appuser

# Copy the fat JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Ensure the app user owns the working directory
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Railway injects PORT environment variable
ENV PORT=8080
EXPOSE ${PORT}

# JVM production tuning:
# - Use container-aware memory settings
# - Enable G1GC for better latency
# - Set timezone
ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+OptimizeStringConcat \
  -Djava.security.egd=file:/dev/./urandom \
  -Duser.timezone=UTC"

# Health check using the existing /api/health endpoint
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:${PORT}/api/health || exit 1

# Run Spring Boot with the PORT env var (Railway sets this automatically)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
