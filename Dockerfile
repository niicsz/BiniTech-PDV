# ============================================
# Stage 1: Build do Frontend Angular
# ============================================
FROM node:20-alpine AS frontend-build

WORKDIR /app/frontend

COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci

COPY frontend/ ./
RUN npm run build -- --configuration=production

# ============================================
# Stage 2: Build do Backend Spring Boot
# ============================================
FROM eclipse-temurin:17-jdk-alpine AS backend-build

WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw

# Baixa dependências (cache de layer)
RUN ./mvnw dependency:go-offline -B

COPY src/ src/

# Copia o build do Angular para os resources estáticos do Spring Boot
COPY --from=frontend-build /app/frontend/dist/binitech-pdv-frontend/browser/ src/main/resources/static/

# Build do JAR sem rodar testes
RUN ./mvnw package -DskipTests -B

# ============================================
# Stage 3: Runtime
# ============================================
FROM eclipse-temurin:17-jre-alpine AS runtime

WORKDIR /app

# Cria usuário não-root para segurança
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=backend-build /app/target/*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

ENV PORT=8080

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]


