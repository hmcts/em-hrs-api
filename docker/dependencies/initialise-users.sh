#!/bin/bash
export IDAM_URI="http://localhost:5001"
export IDAM_USERNAME="idamOwner@hmcts.net"
export IDAM_PASSWORD="Ref0rmIsFun"
export HRS_SYSTEM_USER_NAME="hrs.tester@hmcts.net"
export HRS_SYSTEM_USER_PASSWORD="4590fgvhbfgbDdffm3lk4j"


echo "NOTE AS OF 18th Jan 2022 you get a 403 error instead of a 401 error, when you try to create a user that already exists"


echo "Getting IDAM Authentication Token ..."
token=$(./docker/dependencies/idam-authenticate.sh ${IDAM_URI} ${IDAM_USERNAME} ${IDAM_PASSWORD})
while [ "_${token}" = "_" ]; do
  sleep 10
  echo "idam-api is not running! Check logs, you may need to restart..reattempting in 10 seconds"
  token=$(./docker/dependencies/idam-authenticate.sh ${IDAM_URI} ${IDAM_USERNAME} ${IDAM_PASSWORD})
done


# Set up IDAM client with services and roles
echo "Setting up IDAM clients, users and roles used for ccd import (needs to match the roles in the spreadsheet)"
echo "error 409 means the user is already in the system, so ignore"

echo "Setting up IDAM client...oauth em"
./docker/dependencies/idam-client-setup.sh ${IDAM_URI} services ${token} '{"description": "em", "label": "em", "oauth2ClientId": "webshow", "oauth2ClientSecret": "AAAAAAAAAAAAAAAA", "oauth2RedirectUris": ["http://localhost:8080/oauth2redirect"], "selfRegistrationAllowed": true}'
echo
echo

echo "Setting up IDAM client...oauth ccd"
./docker/dependencies/idam-client-setup.sh ${IDAM_URI} services ${token} '{"description": "ccd gateway", "label": "ccd gateway", "oauth2ClientId": "ccd_gateway", "oauth2ClientSecret": "AAAAAAAAAAAAAAAA", "oauth2RedirectUris": ["http://localhost:3451/oauth2redirect"], "selfRegistrationAllowed": true}'
echo
echo

./docker/dependencies/idam-client-setup-roles.sh ${IDAM_URI} ${token} caseworker

./docker/dependencies/idam-client-setup-roles.sh ${IDAM_URI} ${token} caseworker-hrs

./docker/dependencies/idam-client-setup-roles.sh ${IDAM_URI} ${token} caseworker-hrs-searcher

./docker/dependencies/idam-client-setup-roles.sh ${IDAM_URI} ${token} ccd-import

./docker/dependencies/idam-create-hrs-system-user.sh ${IDAM_URI} ${HRS_SYSTEM_USER_NAME} ${HRS_SYSTEM_USER_PASSWORD}
