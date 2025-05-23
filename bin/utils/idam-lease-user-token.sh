#!/usr/bin/env bash

set -euo pipefail

if [ $# -lt 2 ]; then
    echo "Usage: $0 <username> <password>"
    exit 1
fi

username="${1}"
password="${2}"

# Use environment variables set by the parent script or fall back to defaults
IDAM_API_URL="${IDAM_API_BASE_URI:-http://localhost:5000}"
IDAM_STUB_LOCALHOST="${IDAM_STUB_LOCALHOST:-}"
IDAM_URL="${IDAM_STUB_LOCALHOST:-$IDAM_API_URL}"
CLIENT_ID="${CCD_API_GATEWAY_IDAM_CLIENT:-ccd_gateway}"
CLIENT_SECRET="${CCD_API_GATEWAY_IDAM_CLIENT_SECRET:-ccd_gateway_secret}"
REDIRECT_URI="${CCD_IDAM_REDIRECT_URL:-http://localhost:3451/oauth2redirect}"

# Check if we're using the stub IDAM
if [ -n "${IDAM_STUB_LOCALHOST:-}" ]; then
    echo "Using stubbed IDAM at ${IDAM_URL}"
    echo "stubbed-user-token"
    exit 0
fi

# Get authorization code
echo "Requesting authorization code from ${IDAM_URL}/oauth2/authorize..."
if ! code=$(curl --insecure --fail --show-error --silent --max-time 30 \
    -X POST \
    --user "${username}:${password}" \
    "${IDAM_URL}/oauth2/authorize?redirect_uri=${REDIRECT_URI}&response_type=code&client_id=${CLIENT_ID}" \
    -d "" | docker run --rm --interactive ghcr.io/jqlang/jq:latest -r .code); then
    echo "Failed to get authorization code from IDAM"
    exit 1
fi

if [ -z "$code" ] || [ "$code" = "null" ]; then
    echo "Failed to extract authorization code from IDAM response"
    exit 1
fi

# Exchange authorization code for access token
echo "Exchanging authorization code for access token..."
if ! token=$(curl --insecure --fail --show-error --silent --max-time 30 \
    -X POST \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --user "${CLIENT_ID}:${CLIENT_SECRET}" \
    "${IDAM_URL}/oauth2/token?code=${code}&redirect_uri=${REDIRECT_URI}&grant_type=authorization_code" \
    -d "" | docker run --rm --interactive ghcr.io/jqlang/jq:latest -r .access_token); then
    echo "Failed to exchange authorization code for access token"
    exit 1
fi

if [ -z "$token" ] || [ "$token" = "null" ]; then
    echo "Failed to extract access token from IDAM response"
    exit 1
fi

echo "$token"
