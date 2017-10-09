FROM java:8

MAINTAINER marnikitta@gmail.com

COPY target/concierge-frontend-1.0-SNAPSHOT-uber.jar /application.jar
COPY entrypoint.sh /entrypoint.sh

ENV CONCIERGES: ["localhost:23456"]
ENV HOST: localhost
ENV ACTOR_PORT: 23456
ENV CLIENT_PORT: 8080

EXPOSE 23456 8080

ENTRYPOINT ["/entrypoint.sh"]

