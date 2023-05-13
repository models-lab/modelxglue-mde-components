FROM maven:3.8.6-openjdk-11-slim

WORKDIR /

RUN apt-get update \
 && apt-get install wget unzip zip git bash curl -y

RUN wget -O mar.zip https://github.com/mar-platform/mar/archive/refs/heads/jesus-artefact.zip; \
    unzip mar.zip; \
    cd mar-jesus-artefact; \
    wget http://sanchezcuadrado.es/mar/external-resources/libs.tar.gz; \
    tar -xvf libs.tar.gz; \
    bash ./bin/install-deps.sh
    
RUN cd mar-jesus-artefact; \
    mvn package -pl mar-modelling -am -Dmaven.test.skip=true

RUN cd mar-jesus-artefact; \
    mvn package -pl mar-indexer-spark -am -Dmaven.test.skip=true; \
    mvn package -pl mar-restservice -am -Dmaven.test.skip=true;


RUN wget https://repo1.maven.org/maven2/org/jruby/jruby-dist/9.4.1.0/jruby-dist-9.4.1.0-bin.zip
RUN unzip jruby-dist-9.4.1.0-bin.zip
RUN export PATH=$PATH:/jruby-9.4.1.0/bin

RUN mkdir -p /code

COPY fit.rb /code
COPY config.json /code


