#!/bin/bash
date
## Usage: ./docker/dependencies/start-local-environment DOCMOSIS_ACCESS_KEY
##
## Options:
##    - DOCMOSIS_ACCESS_KEY: Access key for docmosis development environment.
##
## Start local environment including idam client setup.

## DOES NOT START UP HRS API

# Set variables
export COMPOSE_FILE="-f docker-compose-dependencies.yml"



export DOCMOSIS_ACCESS_KEY=$1

echo "****************************************************************************************************************"
echo "These dependencies take around 3 minutes to be fully up. (7+ mins if you do not have the docker images yet)"
echo "This script contains sleep delays to allow for dependencies to be ready, to reduce / remove error logs."
echo ""
echo "If you have issues with start up, open another terminal and monitor the logs with docker-compose ${COMPOSE_FILE} logs -f"
echo "This script will end by tailing the logs. Approx 1 more minute is required for all services to be ready"

echo ""
echo "The final log to see, to indicate that ccd simulator is ready is this:"
echo ">>>>> ccd-data-store-api_1    | 2021-04-11T10:54:38.861 INFO  [main] o.s.d.r.c.DeferredRepositoryInitializationListener Spring Data repositories initialized!"
echo ""
echo "Once everything is ready, CCD needs to be initialised with the HRS CCD Defintion"
echo "To prime CCD, run the functional tests with: ./gradlew functional"

echo ""
echo ""
echo "Please note the following expected error in the logs "

echo ">>>>> WARNING: Loading configuration file /opt/openidm/conf/managed.json failed"
echo ">>>>> Caused by: com.fasterxml.jackson.core.JsonParseException: Unexpected character ('^' (code 94)): was expecting double-quote to start field name"
echo ""
echo ""

echo "Some images come from the azure repo, you will need an azure ecr login. Use this CLI command to log in:"
echo "az acr login --name hmctspublic && az acr login --name hmctsprivate"
echo ""
echo "****************************************************************************************************************"


echo "Logging into Azure Container Repository"
az acr login --name hmctspublic && az acr login --name hmctsprivate

echo "Pulling latest containers!"
./docker/dependencies/pull-latest-dependencies.sh

echo "start idam containers"
./docker/dependencies/start-idam-containers.sh


echo "intialise idam users and roles"
./docker/dependencies/intialise-users.sh

# Start all other images
echo "Starting dependencies..."

docker-compose ${COMPOSE_FILE} build
docker-compose ${COMPOSE_FILE} up -d shared-database \
  em-hrs-db \
  service-auth-provider-api \
  azure-storage-emulator-azurite \
  ccd-user-profile-api \
  ccd-definition-store-api \
  ccd-data-store-api \
  ccd-api-gateway \
  smtp-server \
  am-role-assignment-service \
  ccd-case-document-am-api

echo "LOCAL ENVIRONMENT BOOT UP SUCCESSFULLY STARTED, about to tail logs whilst apps intialise. CCD Data API is the longest running to  initialise"



az storage container create --name 'cvptestcontainer' --connection-string 'DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;'



date

echo "Remember to prime CCD with the hrs ccd data definition - by running the functional tests once the CCD data-store API is ready.."
echo "The final log to see, to indicate that ccd-data-store-api  is ready is this:"
echo "ccd-data-store-api_1    | 2021-04-11T10:54:38.861 INFO  [main] o.s.d.r.c.DeferredRepositoryInitializationListener Spring Data repositories initialized!"
echo ""
read -p "Press the ENTER key to continue"

echo "uploading test file until this is done as part of the tests"

#echo "adding all remaining services (if any)"
#docker-compose ${COMPOSE_FILE} up -d --scale em-hrs-api=0

docker-compose ${COMPOSE_FILE} logs -f
