FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY settings.gradle build.gradle ./
COPY inventory-service/build.gradle inventory-service/build.gradle
COPY order-service/build.gradle order-service/build.gradle
COPY gateway/build.gradle gateway/build.gradle
RUN ./gradlew :inventory-service:dependencies --no-daemon || true
COPY inventory-service/src inventory-service/src
RUN ./gradlew :inventory-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --create-home appuser
COPY --from=build /app/inventory-service/build/libs/*-SNAPSHOT.jar app.jar
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
