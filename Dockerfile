# ------------ BUILD STAGE ------------
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests


# ------------ RUN STAGE ------------
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (Render uses PORT env var)
EXPOSE 8080

ENV PORT=8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
