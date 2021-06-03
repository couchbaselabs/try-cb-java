#!/bin/bash
# wait-for-couchbase.sh

set -e

CB_HOST="${CB_HOST:-db}"
CB_USER="${CB_USER:-Administrator}"
CB_PSWD="${CB_PSWD:-password}"

#### Utility Functions ####
# (see bottom of file for the calling script)

log() {
  echo "wait-for-couchbase: $@" | cut -c -${COLUMNS:-80}
}

wait-for-one() {
  local ATTEMPTS=$1
  local URL=$2
  local QUERY=$3
  local EXPECTED=true
  local OUT=wait-for-couchbase.out

  for attempt in $(seq 1 $ATTEMPTS); do
    status=$(curl -s -w "%{http_code}" -o $OUT -u "${CB_USER}:${CB_PSWD}" $URL)
    if [ $attempt -eq 1 ]; then
      log "polling for '$QUERY'"
      if [ $DEBUG ]; then jq . $OUT; fi
    elif (($attempt % 5 == 0)); then
      log "..."
    fi
    if [ "x$status" == "x200" ]; then
      result=$(jq "$QUERY" <$OUT)
      if [ "x$result" == "x$EXPECTED" ]; then
        return # success
      fi
      if [ $attempt -eq 1 ]; then
        log "value is currently:"
        jq . <<<"$result"
      fi
    fi
    sleep 2
  done
  return 1 # failure
}

wait-for() {
  local ATTEMPTS=$1
  local URL="http://${CB_HOST}${2}"
  shift
  shift

  log "checking $URL"

  for QUERY in "$@"; do
    wait-for-one $ATTEMPTS $URL "$QUERY" || (
      log "Failure"
      exit 1
    )
  done
  return # success
}

function createHotelsIndex() {
  log "Creating hotels-index..."
  http_code=$(curl -o hotel-index.out -w '%{http_code}' -s -u ${CB_USER}:${CB_PSWD} -X PUT \
    http://${CB_HOST}:8094/api/index/hotels-index \
    -H 'cache-control: no-cache' \
    -H 'content-type: application/json' \
    -d @fts-hotels-index.json)
  if [[ $http_code -ne 200 ]]; then
    log Hotel index creation failed
    cat hotel-index.out
    exit 1
  fi
}

##### Script starts here #####
ATTEMPTS=150

wait-for $ATTEMPTS \
  ":8091/pools/default/buckets/travel-sample/scopes/" \
  '.scopes | map(.name) | contains(["inventory", "tenant_agent_00", "tenant_agent_01"])'

wait-for $ATTEMPTS \
  ":8094/api/cfg" \
  '.status == "ok"'

if (wait-for 1 ":8094/api/index/hotels-index" '.status == "ok"'); then
  log "index already exists"
else
  createHotelsIndex
  wait-for $ATTEMPTS \
    ":8094/api/index/hotels-index/count" \
    '.count >= 917'
fi

# now check that the indexes have had enough time to come up...
wait-for $ATTEMPTS \
  ":9102/api/v1/stats" \
  '.indexer.indexer_state == "Active"' \
  '. | keys | contains(["travel-sample:def_airportname", "travel-sample:def_city", "travel-sample:def_faa", "travel-sample:def_icao", "travel-sample:def_name_type", "travel-sample:def_primary", "travel-sample:def_route_src_dst_day", "travel-sample:def_schedule_utc", "travel-sample:def_sourceairport", "travel-sample:def_type", "travel-sample:inventory:airline:def_inventory_airline_primary", "travel-sample:inventory:airport:def_inventory_airport_airportname", "travel-sample:inventory:airport:def_inventory_airport_city", "travel-sample:inventory:airport:def_inventory_airport_faa", "travel-sample:inventory:airport:def_inventory_airport_primary", "travel-sample:inventory:hotel:def_inventory_hotel_city", "travel-sample:inventory:hotel:def_inventory_hotel_primary", "travel-sample:inventory:landmark:def_inventory_landmark_city", "travel-sample:inventory:landmark:def_inventory_landmark_primary", "travel-sample:inventory:route:def_inventory_route_primary", "travel-sample:inventory:route:def_inventory_route_route_src_dst_day", "travel-sample:inventory:route:def_inventory_route_schedule_utc", "travel-sample:inventory:route:def_inventory_route_sourceairport"])' \
  '. | del(.indexer) | del(.["travel-sample:def_name_type"]) | map(.items_count > 0) | all' \
  '. | del(.indexer) | map(.num_pending_requests == 0) | all'

exec $@
