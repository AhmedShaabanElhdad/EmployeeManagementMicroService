FROM eclipse-temurin:17-jdk-alpine as build
WORKDIR /workspace/app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY shared shared
COPY api-gateway api-gateway

RUN ./gradlew :api-gateway:assemble -x test

FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
COPY --from=build /workspace/app/api-gateway/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
