FROM eclipse-temurin:17-jdk as build
WORKDIR /workspace/app

# Copy gradle executable and configuration
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy all project structures (just the directories and build files) to satisfy Gradle configuration
COPY shared/build.gradle shared/
COPY api-gateway/build.gradle api-gateway/
COPY Auth-service/build.gradle Auth-service/
COPY Eureka-server/build.gradle Eureka-server/
COPY Employee-service/build.gradle Employee-service/
COPY Department-service/build.gradle Department-service/
COPY Notification/build.gradle Notification/
COPY Payroll-service/build.gradle Payroll-service/

# Copy the source code for the required modules
COPY shared shared
COPY Department-service Department-service

# Build the specific module
RUN ./gradlew :Department-service:assemble -x test

FROM eclipse-temurin:17-jre
COPY --from=build /workspace/app/Department-service/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
