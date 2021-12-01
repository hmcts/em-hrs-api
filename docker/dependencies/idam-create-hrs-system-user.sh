#!/bin/bash

## Usage: ./docker/dependencies/idam-authenticate.sh IDAM_URI SYSTEM_USERNAME SYSTEM_PASSWORD
##
##
## Make call to IDAM to get auth token

IDAM_URI=$1
USERNAME=$2
PASSWORD=$3

DATA='{"email":"'${USERNAME}'", "password":"'${PASSWORD}'", "surname":"system" , "forename":"user", "roles":[{"code":"caseworker"},{"code":"caseworker-hrs-searcher"}]}'

curl -XPOST "${IDAM_URI}/testing-support/accounts" \
     -H "Content-Type: application/json" \
     -d "${DATA}"
