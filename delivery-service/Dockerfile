# Stage 1: build the fat jar
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY order-service/pom.xml ./order-service/
COPY tracking-service/pom.xml ./tracking-service/
COPY delivery-service/pom.xml ./delivery-service/

COPY delivery-service/src ./delivery-service/src

RUN mvn -B -pl delivery-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/delivery-service/target/delivery-service-1.0.0.jar app.jar
EXPOSE 8083
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
