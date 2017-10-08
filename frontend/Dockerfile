FROM java:8

MAINTAINER marnikitta@gmail.com

COPY target/concierge-frontend-1.0-SNAPSHOT-uber.jar /application.jar
COPY entrypoint.sh /entrypoint.sh

ENV CONCIERGES: localhost
ENV HOST: localhost

EXPOSE 23456 8080

ENTRYPOINT ["/entrypoint.sh"]

