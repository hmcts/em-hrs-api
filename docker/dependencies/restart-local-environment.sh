#!/bin/bash
date
COMPOSE_FILE="-f docker-compose-dependencies.yml"

echo "compose file $COMPOSE_FILE"

docker-compose ${COMPOSE_FILE} up -d --scale em-hrs-api=0
docker-compose ${COMPOSE_FILE} logs -f
