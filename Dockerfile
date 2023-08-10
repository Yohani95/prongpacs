# Imagen base de Maven para construir el proyecto
FROM maven:3.6.3-openjdk-11 AS builder

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copia el archivo pom.xml y las dependencias en un paso separado
COPY pom.xml pom.xml
RUN mvn dependency:go-offline

# Copia el código fuente
COPY src src

# Compila el proyecto y crea el archivo JAR
RUN mvn package -DskipTests

# Imagen base de Java para ejecutar el proyecto
FROM adoptopenjdk:11-jre-hotspot

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copia el archivo JAR desde la imagen anterior
COPY --from=builder /app/target/prongpacs*.jar ./prongpacs.jar

# Crea la carpeta "logs" dentro del contenedor
RUN mkdir /app/logs

# Comando para ejecutar el JAR
CMD ["java", "-jar", "prongpacs.jar"]
EXPOSE 8085