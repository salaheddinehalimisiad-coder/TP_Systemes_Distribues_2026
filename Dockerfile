# Étape 1 : Build avec Maven (crée un fat JAR avec toutes les dépendances)
FROM maven:3.8.4-openjdk-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -q

# Étape 2 : Image légère pour l'exécution
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/Messagerie-full.jar ./app.jar
COPY src/main/resources/web ./src/main/resources/web
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "8080"]
