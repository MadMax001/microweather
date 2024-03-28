FROM maven:3.8.5-openjdk-17 AS MAVEN_BUILD

ARG RELATIVE_MODULE

ENV HOME=/home/app
RUN mkdir -p $HOME

WORKDIR $HOME

ADD pom.xml $HOME/pom.xml
ADD mc-common/pom.xml $HOME/mc-common/pom.xml
ADD mc-common-test/pom.xml $HOME/mc-common-test/pom.xml
ADD mc-consumer-db/pom.xml $HOME/mc-consumer-db/pom.xml
ADD mc-consumer-second/pom.xml $HOME/mc-consumer-second/pom.xml
ADD mc-producer/pom.xml $HOME/mc-producer/pom.xml
ADD mc-remote-currate/pom.xml $HOME/mc-remote-currate/pom.xml
RUN ["/usr/local/bin/mvn-entrypoint.sh", "mvn", "verify", "clean", "--fail-never"]

ADD mc-common $HOME/mc-common/
ADD mc-common-test $HOME/mc-common-test/
ADD ${RELATIVE_MODULE} /$HOME/${RELATIVE_MODULE}/

ARG PV_ARG
ENV PV=${PV_ARG}

RUN mvn -pl ${RELATIVE_MODULE} -DskipTests -DPROJECT_VERSION=$PV --also-make package

FROM openjdk:17-jdk-alpine
WORKDIR /opt/app/

ARG RELATIVE_MODULE

ENV SERVICE_NAME=${RELATIVE_MODULE}
ENV HOME=/home/app

COPY --from=MAVEN_BUILD $HOME/${RELATIVE_MODULE}/target/${RELATIVE_MODULE}.jar ${RELATIVE_MODULE}.jar
CMD java -jar /opt/app/$SERVICE_NAME.jar
