FROM maven:3.8.3-openjdk-11

WORKDIR /

RUN apt-get update \
 && apt-get install python bash jq -y

RUN mkdir -p /scripts

COPY ecore-to-tree /scripts/ecore-to-tree

COPY transform.py /scripts

RUN cd /scripts/ecore-to-tree && \
    mvn package -Dmaven.test.skip=true

