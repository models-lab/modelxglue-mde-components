FROM maven:3.8.6-openjdk-11-slim

WORKDIR /

RUN apt-get update \
 && apt-get install bash jq -y

RUN mkdir -p /lib
RUN mkdir -p /scripts

COPY lib/MemoRecLite-1.0.jar /lib
COPY scripts/extract.sh scripts
COPY scripts/recommend.sh scripts

RUN chmod +x /scripts/extract.sh
RUN chmod +x /scripts/recommend.sh

