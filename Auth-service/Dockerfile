FROM eclipse-temurin:17-jdk as build
WORKDIR /workspace/app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy build files for all modules to satisfy Gradle configuration
COPY shared/build.gradle shared/
COPY api-gateway/build.gradle api-gateway/
COPY Auth-service/build.gradle Auth-service/
COPY Eureka-server/build.gradle Eureka-server/
COPY Employee-service/build.gradle Employee-service/
COPY Department-service/build.gradle Department-service/
COPY Notification/build.gradle Notification/
COPY Payroll-service/build.gradle Payroll-service/

# Copy source code for required modules
COPY shared shared
COPY Auth-service Auth-service

RUN ./gradlew :Auth-service:assemble -x test

FROM eclipse-temurin:17-jre
COPY --from=build /workspace/app/Auth-service/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
