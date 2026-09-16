# Stage 1: build the fat jar
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Pre-fetch the parent POM so child module builds don't need a registry lookup
COPY pom.xml ./
COPY order-service/pom.xml ./order-service/
COPY tracking-service/pom.xml ./tracking-service/
COPY delivery-service/pom.xml ./delivery-service/

# Download dependencies first (cache layer)
RUN mvn -B -pl order-service -am dependency:go-offline || true

# Copy source
COPY order-service/src ./order-service/src

# Build
RUN mvn -B -pl order-service -am clean package -DskipTests

# Stage 2: runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/order-service/target/order-service-1.0.0.jar app.jar
EXPOSE 8081
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
