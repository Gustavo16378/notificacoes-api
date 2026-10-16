# Etapa 1: build da aplicação
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copia apenas o pom e baixa dependências (melhor uso de cache)
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Copia o código-fonte
COPY src ./src

# Compila e empacota a aplicação (gera o JAR)
RUN mvn -q clean package -DskipTests

# Etapa 2: imagem leve para rodar
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copia o JAR gerado da etapa de build
COPY --from=build /app/target/*.jar app.jar

# Porta interna do Spring Boot
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]