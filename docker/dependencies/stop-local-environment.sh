#!/bin/bash

## Usage: ./docker/dependencies/stop-local-environment

## Stop local environment

# Set variables
COMPOSE_FILE="-f docker-compose-ccd-dependencies.yml"


docker-compose ${COMPOSE_FILE} down
echo "LOCAL ENVIRONMENT SUCCESSFULLY STOPPED"
