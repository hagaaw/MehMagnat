
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app


COPY pom.xml ./
RUN mvn dependency:go-offline -B


COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring


RUN mkdir -p /uploads && chown spring:spring /uploads

USER spring:spring

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75", \
  "-jar", "app.jar"]
