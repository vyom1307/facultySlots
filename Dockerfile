# ------------ BUILD STAGE ------------
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the Maven project from facultySlots folder
COPY facultySlots/pom.xml ./
COPY facultySlots/src ./src

# Build the Spring Boot application
RUN mvn clean package -DskipTests


# ------------ RUN STAGE ------------
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy the generated JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose Render port
ENV PORT=8080
EXPOSE 8080

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
