FROM openjdk:8u102-jre
ARG finalName
ADD $finalName /try-cb-java.jar
VOLUME /tmp
ENTRYPOINT ["java","-jar","/try-cb-java.jar"]

