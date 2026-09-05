FROM eclipse-temurin:21-jdk AS build
WORKDIR /build
COPY mvnw ./
COPY .mvn .mvn
COPY auth-service/pom.xml auth-service/pom.xml
RUN chmod +x mvnw && ./mvnw -f auth-service/pom.xml dependency:go-offline -B
COPY auth-service/src auth-service/src
RUN ./mvnw -f auth-service/pom.xml package -DskipTests -B

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
COPY --from=build --chown=appuser:appgroup /build/auth-service/target/auth-service-*.jar app.jar
USER appuser
ENV PORT=8081
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
