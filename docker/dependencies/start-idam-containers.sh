#!/bin/bash
# Start IDAM setup
export COMPOSE_FILE="-f docker-compose-dependencies.yml"


echo "Starting shared-db..."
docker-compose ${COMPOSE_FILE} up -d shared-db
echo "sleeping 10 seconds to allow db to be ready"
sleep 10
#read -p "Press enter to continue 0".

echo "Starting IDAM(ForgeRock)..."
docker-compose ${COMPOSE_FILE} up -d fr-am
echo "sleeping 30 seconds to allow idam fr-AM to be ready"
sleep 30
#read -p "Press enter to continue idam forge rock".

docker-compose ${COMPOSE_FILE} up -d fr-idm
echo "Launching fr-idm - dont worry about this exception: org.forgerock.json.JsonException: String passed into parsing is not valid JSON"
echo "sleeping 5 seconds to allow idam fr-idm to be ready"
sleep 5
#read -p "Press enter to continue 1".

echo "Starting IDAM API..."
docker-compose ${COMPOSE_FILE} up -d idam-api
echo "sleeping 40 seconds to allow idam API to be ready. If anything fails, just cancel and rerun this script"
sleep 40
#read -p "Press enter to continue".
