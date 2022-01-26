#!/bin/bash

## Usage: ./docker/dependencies/idam-authenticate.sh IDAM_URI SYSTEM_USERNAME SYSTEM_PASSWORD
##
##
## Make call to IDAM to get auth token

IDAM_URI=$1
USERNAME=$2
PASSWORD=$3

echo "Creatinng User:"
echo "IDAM_URI: $IDAM_URI"
echo "USERNAME: $USERNAME"
echo "PASSWORD: $PASSWORD"



DATA='{"email":"'${USERNAME}'", "password":"'${PASSWORD}'", "surname":"system" , "forename":"user", "roles":[{"code":"caseworker"},{"code":"caseworker-hrs"},{"code":"caseworker-hrs-searcher"}]}'

echo "JSON DATA: $DATA"

curl -XPOST  "${IDAM_URI}/testing-support/accounts" \
     -H "accept: */*" \
     -H "Content-Type: application/json" \
     -d "${DATA}"
