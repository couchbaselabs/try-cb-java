#!/bin/bash

# exit immediately if a command fails or if there are unset vars
set -euo pipefail

CB_USER="${CB_USER:-Administrator}"
CB_PSWD="${CB_PSWD:-password}"
CB_HOST="${CB_HOST:-db}"

function createHotelsIndex() {
    echo
    echo "Creating hotels-index..."
    curl --fail -s -u ${CB_USER}:${CB_PSWD} -X PUT \
        http://${CB_HOST}:8094/api/index/hotels-index \
        -H 'cache-control: no-cache' \
        -H 'content-type: application/json' \
        -d @fts-hotels-index.json
    echo

    echo "Waiting for hotels-index to be ready. Please wait..."
    until curl --fail -s -u ${CB_USER}:${CB_PSWD} http://${CB_HOST}:8094/api/index/hotels-index/count |
        jq -e '.count' | grep 917 >/dev/null; do
        echo "Waiting for hotels-index to be ready. Trying again in 10 seconds."
        sleep 10
    done

    echo "Done."
}

echo
echo "Checking 'hotels-index' setup..."
if curl --fail -s -o /dev/null -u ${CB_USER}:${CB_PSWD} http://${CB_HOST}:8094/api/index/hotels-index; then
    echo
    echo "Index already exists, carrying on..."
else
    createHotelsIndex
fi

echo
echo "Running backend..."
mvn spring-boot:run -Dspring-boot.run.arguments="--storage.host=$CB_HOST --storage.username=$CB_USER --storage.password=$CB_PSWD"
