
# create bucket
curl -X POST -u Administrator:password http://localhost:8091/pools/default/buckets -d name=default -d ramQuotaMB=100

# create scopes
curl -u Administrator:password -X POST http://localhost:8091/pools/default/buckets/default/collections -d name=larson-travel
curl -u Administrator:password -X POST http://localhost:8091/pools/default/buckets/default/collections -d name=simmons-travel

# create collections in each scope
curl -u Administrator:password -X POST http://localhost:8091/pools/default/buckets/default/collections/larson-travel -d name=users
curl -u Administrator:password -X POST http://localhost:8091/pools/default/buckets/default/collections/larson-travel -d name=trips
curl -u Administrator:password -X POST http://localhost:8091/pools/default/buckets/default/collections/larson-travel -d name=flights
curl -u Administrator:password -X POST http://localhost:8091/pools/default/buckets/default/collections/larson-travel -d name=stays

curl -u Administrator:password -X POST http://localhost:8091/pools/default/buckets/default/collections/simmons-travel -d name=users
curl -u Administrator:password -X POST http://localhost:8091/pools/default/buckets/default/collections/simmons-travel -d name=trips
curl -u Administrator:password -X POST http://localhost:8091/pools/default/buckets/default/collections/simmons-travel -d name=flights
curl -u Administrator:password -X POST http://localhost:8091/pools/default/buckets/default/collections/simmons-travel -d name=stays

# show what we have made
echo '\n\n\n\nTHE FINAL RESULT'
curl -u Administrator:password -X GET http://localhost:8091/pools/default/buckets/default/collections
echo '\n'
