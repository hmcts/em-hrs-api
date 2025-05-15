#!/usr/bin/env bash

set -euo pipefail

if [ $# -lt 2 ]; then
    echo "Usage: $0 <microservice> <oneTimePassword>"
    exit 1
fi

microservice="${1}"
oneTimePassword="${2}"

# Use environment variables set by the parent script or fall back to defaults
S2S_URL="${S2S_URL:-http://localhost:4502}"

# Ensure IDAM_STUB_LOCALHOST is set with a default empty value
IDAM_STUB_LOCALHOST="${IDAM_STUB_LOCALHOST:-}"

# Check if we're using stubbed S2S
if [ -n "${IDAM_STUB_LOCALHOST}" ]; then
    echo "Using stubbed S2S at ${S2S_URL}"
    echo "stubbed-service-token"
    exit 0
fi

echo "Requesting service token from ${S2S_URL}/lease..."
if ! response=$(curl --insecure --fail --show-error --silent --max-time 30 \
    -X POST \
    -H "Content-Type: application/json" \
    "${S2S_URL}/lease" \
    -d '{
        "microservice": "'${microservice}'",
        "oneTimePassword": "'${oneTimePassword}'"
    }' 2>&1); then
    echo "Failed to get service token from S2S: ${response}"
    exit 1
fi

# Extract the token from the response
if ! token=$(echo "${response}" | tr -d '\r\n'); then
    echo "Failed to extract service token from response: ${response}"
    exit 1
fi

if [ -z "${token}" ] || [ "${token}" = "null" ]; then
    echo "Failed to get valid service token from S2S response: ${response}"
    exit 1
fi

echo "${token}"
