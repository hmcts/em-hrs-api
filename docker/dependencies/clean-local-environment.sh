#!/bin/bash

## Usage: ./docker/dependencies/stop-local-environment

## Stop local environment

# Set variables
COMPOSE_FILE="-f docker-compose-dependencies.yml"


docker-compose ${COMPOSE_FILE} down -v
echo "LOCAL ENVIRONMENT SUCCESSFULLY STOPPED"
