FROM maven:3.8.6-openjdk-11-slim

WORKDIR /

RUN mkdir -p /scripts/lucene

COPY lucene /scripts/lucene

RUN cd /scripts/lucene; \
    mvn package -Dmaven.test.skip=true



