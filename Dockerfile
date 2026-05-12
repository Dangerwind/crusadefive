FROM eclipse-temurin:21-jdk AS build

WORKDIR /app
COPY . .

ENV GRADLE_OPTS="-Xmx512m"

RUN chmod +x gradlew
RUN ./gradlew clean build -x test --no-daemon

FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]