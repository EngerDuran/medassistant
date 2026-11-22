# ==========================================================================
# Etapa 1: Build — Compilación con Maven y JDK 21 Multi-stage build
# ==========================================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Cacheo de capas: copiamos solo el pom.xml primero. Si las dependencias no
# cambian, Docker reutiliza esta capa y acelera los builds futuros.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiamos el código fuente y empaquetamos el artefacto final
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==========================================================================
# Etapa 2: Runtime — Imagen ligera solo con JRE 21 para producción
# ==========================================================================
FROM eclipse-temurin:21-jre-jammy

# Configuración de zona horaria para sincronizar logs de auditoría
# y asegurar que LocalDate.now() en los prompts del LLM opere en hora local.
ENV TZ=Europe/Madrid
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Seguridad: creamos un usuario sin privilegios (non-root) para limitar
# la superficie de ataque en caso de vulnerabilidad en el contenedor.
RUN groupadd -r spring && useradd -r -g spring spring

WORKDIR /app

# Copiamos únicamente el .jar compilado desde la etapa anterior, descartando
# Maven y el código fuente para reducir el peso de la imagen en AWS.
COPY --from=builder /build/target/*.jar app.jar

RUN chown -R spring:spring /app

USER spring

# Activamos por defecto el perfil de producción configurado para AWS RDS y pgvector
ENV SPRING_PROFILES_ACTIVE=aws

EXPOSE 8080

# Ajustamos la memoria de la JVM al 75% del límite del contenedor para evitar
# errores OOMKilled out Of Memory en instancias EC2 ajustadas de RAM.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]