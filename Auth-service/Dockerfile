FROM eclipse-temurin:17-jdk as build
WORKDIR /workspace/app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY shared shared
COPY Auth-service Auth-service

RUN ./gradlew :Auth-service:assemble -x test

FROM eclipse-temurin:17-jre
COPY --from=build /workspace/app/Auth-service/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
