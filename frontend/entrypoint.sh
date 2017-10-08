#!/usr/bin/env bash

printenv

java -jar /application.jar "${HOST}:${PORT}" "${API_PORT}" "${CONCIERGES}"
