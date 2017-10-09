#!/usr/bin/env bash

printenv

cat << EOF > /config.json
{
  "hostname": "${HOST}",
  "actor_port": ${ACTOR_PORT},
  "client_port": ${CLIENT_PORT},
  "heartbeat": 3000,
  "concierges": ${CONCIERGES}
}
EOF

java -jar /application.jar /config.json
