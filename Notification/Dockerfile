FROM eclipse-temurin:17-jdk as build
WORKDIR /workspace/app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY Notification Notification

RUN ./gradlew :Notification:assemble -x test

FROM eclipse-temurin:17-jre
COPY --from=build /workspace/app/Notification/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
