# build
FROM maven:3.6.3-jdk-8 AS builder
#WORKDIR /usr/src/solace/sempv1-interface

#COPY sempv1-interface/pom.xml .
#RUN mvn -B -e -C -T 1C dependency:go-offline

#COPY sempv1-interface/  .
#RUN mvn -B -e -o -T 1C verify install -DskipTests=True

WORKDIR /usr/src/solace/metricscollector

COPY pom.xml .
COPY / .

#WORKDIR /usr/src/solace/metricscollector/lib
#RUN mvn install:install-file -Dfile=sempv1-interface-0.2.0-SNAPSHOT.jar -DgroupId=com.solace.psg -DartifactId=sempv1-interface -Dversion=0.2.0-SNAPSHOT -Dpackaging=jar

#WORKDIR /usr/src/solace/metricscollector
#RUN mvn -B -e -C -T 1C dependency:go-offline

COPY /  .
RUN mvn -B -e -T 1C clean compile package -DskipTests=True

FROM openjdk:8-jdk-alpine
COPY --from=builder /usr/src/solace/metricscollector/target/*.jar app.jar
COPY --from=builder /usr/src/solace/metricscollector/target/config.properties config.properties
COPY --from=builder /usr/src/solace/metricscollector/target/lib/ ./lib
ENTRYPOINT ["java","-jar","app.jar"]
