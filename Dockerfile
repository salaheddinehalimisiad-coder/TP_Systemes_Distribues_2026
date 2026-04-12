# Étape 1 : Build avec Maven
FROM maven:3.8.4-openjdk-17
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn compile
EXPOSE 8080
ENTRYPOINT ["mvn", "exec:java", "-Dexec.mainClass=org.example.web.MailRestController", "-Dexec.args=8080"]
