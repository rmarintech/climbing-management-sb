# Enrollment Service Dockerfile
# ----------------------------------------
# FROM eclipse-temurin:21-jdk-alpine
#        ↓
# Java only
#        ↓
# Maven not installed
#        ↓
# use ./mvnw

# Course application
#   → Maven builder image
#   → mvn
# Enrollment Service
#   → JDK builder image
#   → Maven Wrapper
#   → ./mvnw


# =========================
# 1. BUILD STAGE
# =========================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw

# Download dependencies separately so Docker can reuse this layer
# when source code changes but pom.xml does not.
RUN ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw clean package -DskipTests


# =========================
# 2. RUNTIME STAGE
# =========================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Run the application as a non-root user.
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=builder /app/target/*.jar app.jar

RUN chown spring:spring app.jar

USER spring

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]