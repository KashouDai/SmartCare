FROM maven:3.8.8-eclipse-temurin-8 AS build

WORKDIR /build
COPY backend/hims-backend-code-master/ ./
RUN mvn -DskipTests clean package

FROM eclipse-temurin:8-jre-jammy

WORKDIR /app
COPY --from=build /build/hims-admin/target/hims-admin.jar /app/app.jar
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_OPTS=""
EXPOSE 9999
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
