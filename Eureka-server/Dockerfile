FROM eclipse-temurin:17-jdk as build
WORKDIR /workspace/app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY Eureka-server Eureka-server
RUN ./gradlew :Eureka-server:assemble -x test

FROM eclipse-temurin:17-jre
COPY --from=build /workspace/app/Eureka-server/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
