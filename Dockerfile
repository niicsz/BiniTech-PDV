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
FROM eclipse-temurin:17-jdk AS backend-build

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
FROM eclipse-temurin:17-jre AS runtime

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates && \
    update-ca-certificates && \
    rm -rf /var/lib/apt/lists/* && \
    find /etc/ssl/certs -name "*.pem" -exec \
      sh -c 'keytool -import -trustcacerts -cacerts -storepass changeit -noprompt -alias "$(basename "$1" .pem)" -file "$1" 2>/dev/null || true' _ {} \;

# Cria usuário não-root para segurança
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --from=backend-build /app/target/*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

ENV PORT=8080

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djavax.net.ssl.trustStore=/opt/java/openjdk/lib/security/cacerts", \
  "-Djavax.net.ssl.trustStorePassword=changeit", \
  "-Djdk.tls.client.protocols=TLSv1.2,TLSv1.3", \
  "-jar", "app.jar"]
