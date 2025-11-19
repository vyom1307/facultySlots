# ------------ BUILD STAGE ------------
FROM maven:3.9.4-eclipse-temurin-21 AS build
WORKDIR /app

COPY facultySlots/pom.xml ./
COPY facultySlots/src ./src

RUN mvn clean package -DskipTests


# ------------ RUN STAGE ------------
FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
