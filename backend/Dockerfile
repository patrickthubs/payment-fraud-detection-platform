FROM maven:3.9.11-eclipse-temurin-25 AS maven-distribution

FROM eclipse-temurin:26-jdk AS build
ENV MAVEN_HOME=/usr/share/maven
ENV PATH="${MAVEN_HOME}/bin:${PATH}"
WORKDIR /workspace

COPY --from=maven-distribution /usr/share/maven /usr/share/maven
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -Dmaven.test.skip=true package

FROM eclipse-temurin:26-jre-alpine AS runtime
RUN apk add --no-cache curl \
    && addgroup -S fraud \
    && adduser -S fraud -G fraud
WORKDIR /app

COPY --from=build --chown=fraud:fraud /workspace/target/payment-fraud-backend-*.jar app.jar

USER fraud
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/urandom", "-jar", "/app/app.jar"]
