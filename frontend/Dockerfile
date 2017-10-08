FROM java:8

MAINTAINER marnikitta@gmail.com

COPY target/concierge-frontend-1.0-SNAPSHOT-uber.jar /application.jar
COPY entrypoint.sh /entrypoint.sh

ENV CONCIERGES: localhost:23456
ENV HOST: localhost
ENV PORT: 23456
ENV API_PORT: 8080

ENTRYPOINT ["/entrypoint.sh"]

