#!/bin/bash

GH_USER=${GH_USER:-couchbaselabs}
GH_PROJECT=${GH_PROJECT:-try-cb-python}
GH_BRANCH=${GH_BRANCH:-7.0}

URL=https://raw.githubusercontent.com/${GH_USER}/${GH_PROJECT}/${GH_BRANCH}/swagger.json
echo "Getting $URL ..."
curl -S --fail -O $URL

if [ $? -eq 0 ]; then
    echo swagger.json retrieved:
    git diff --exit-code ./src/main/resources/static/swagger.json && echo "No changes!"
else
    echo "couldn't retrieve swagger.json"
fi
