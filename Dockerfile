FROM openjdk:8u292-slim-buster

LABEL maintainer="Couchbase"

WORKDIR /app

RUN mkdir -p /usr/share/man/man1
RUN apt-get update && apt-get install -y \
    maven \
    jq curl

ADD . /app

# Install project dependencies and generate jar file
RUN mvn clean install

# Expose ports
EXPOSE 8080

# Set the entrypoint
ENTRYPOINT ["./wait-for-couchbase.sh", "java", "-jar", "target/try-cb-java.jar"]
