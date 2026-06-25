FROM eclipse-temurin:21-jdk AS backend-build

WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline -B

COPY src/ src/

RUN ./mvnw package -DskipTests -B

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

RUN rm -rf /var/lib/apt/lists/* && \
    apt-get clean && \
    apt-get update -o Acquire::CompressionTypes::Order::=gz && \
    apt-get install -y --no-install-recommends ca-certificates curl && \
    update-ca-certificates && \
    rm -rf /var/lib/apt/lists/*

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --from=backend-build /app/target/*.jar app.jar

RUN chown appuser:appgroup app.jar
USER appuser

ENV PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:${PORT}/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-Djavax.net.ssl.trustStore=/opt/java/openjdk/lib/security/cacerts", \
  "-Djavax.net.ssl.trustStorePassword=changeit", \
  "-Djdk.tls.client.protocols=TLSv1.2,TLSv1.3", \
  "-jar", "app.jar"]
