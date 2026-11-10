# ---------------------------------------------------------------
# Imagen de UN microservicio a partir del proyecto Maven multimódulo (mismo esquema que la referencia).
# El módulo se elige con ARG MODULE (usuarios, catalogo, pedidos o gateway):
#   docker build --build-arg MODULE=catalogo -t sonido-vivo/catalogo .
# compose.yaml construye las cuatro imágenes con este mismo archivo.
# ---------------------------------------------------------------

# Etapa 1: compilar con Maven y JDK 21 (las pruebas se corren antes, con ./mvnw verify)
FROM maven:3.9.11-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /src
COPY . .
# -pl <módulo> -am: compila solo ese módulo y lo que necesita (common)
RUN mvn -B -ntp -DskipTests -pl ${MODULE} -am package

# Etapa 2: solo el JRE y el .jar (imagen más liviana, sin código fuente ni Maven)
FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY --from=build /src/${MODULE}/target/${MODULE}-1.0.0.jar /app/app.jar
# Usuario sin privilegios: si el proceso se ve comprometido no es root dentro del contenedor
RUN useradd --system --uid 10001 sonidovivo && chown -R sonidovivo /app
USER sonidovivo
EXPOSE 8080 8081 8082 8083
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
