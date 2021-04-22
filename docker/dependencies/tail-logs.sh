#!/bin/bash
date


# Set variables
COMPOSE_FILE="-f docker-compose-dependencies.yml"

docker-compose ${COMPOSE_FILE} logs -f


