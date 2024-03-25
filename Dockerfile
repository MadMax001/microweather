FROM eclipse-temurin:17-jdk-alpine AS MAVEN_BUILD

WORKDIR /opt/app
COPY pom.xml /opt/app/
COPY mc-common /opt/app/mc-common/
COPY mc-common-test /opt/app/mc-common-test/
COPY mc-consumer-db /opt/app/mc-consumer-db/
COPY mc-consumer-second /opt/app/mc-consumer-second/
COPY mc-producer /opt/app/mc-producer/
COPY mc-remote-currate /opt/app/mc-remote-currate/

WORKDIR /tmp/
RUN mvn clean install -f pom.xml -Pcontainers-tests

FROM openjdk:17-jdk-alpine
WORKDIR /opt/app

COPY --from=MAVEN_BUILD /opt/app/mc-consumer-db/target/mc-consumer-db.jar /mc-consumer-db.jar
COPY --from=MAVEN_BUILD /opt/app/mc-consumer-second/target/mc-consumer-second.jar /mc-consumer-second.jar
COPY --from=MAVEN_BUILD /opt/app/mc-producer/target/mc-producer.jar /mc-producer.jar
COPY --from=MAVEN_BUILD /opt/app/mc-remote-currate/target/mc-remote-currate.jar /mc-remote-currate.jar

ENTRYPOINT [java -Ddb.host="${DB_HOST}" -jar /mc-consumer-second.jar]