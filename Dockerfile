# Étape 1 : Build avec Maven
FROM maven:3.8.4-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Étape 2 : Runtime avec JRE
FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/target/Messagerie-1.0-SNAPSHOT.jar app.jar

# Exposer le port par défaut (Sera surchargé par Docker Compose)
EXPOSE 8080

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]
