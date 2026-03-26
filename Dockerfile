# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src ./src

RUN ./mvnw -DskipTests clean package

##
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring

COPY --form=build /workspace/target/*.jar /app/app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
