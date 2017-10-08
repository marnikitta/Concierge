#!/usr/bin/env bash

printenv

java -jar /application.jar "${HOST}" "${CONCIERGES}"
