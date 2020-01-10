
# create bucket
curl -X POST -u $1:$2 http://$3:8091/pools/default/buckets -d name=travel-users -d ramQuotaMB=100

# create scope
curl -u $1:$2 -X POST http://$3:8091/pools/default/buckets/travel-users/collections -d name=userData

# create collections in each scope
curl -u $1:$2 -X POST http://$3:8091/pools/default/buckets/travel-users/collections/userData -d name=users
curl -u $1:$2 -X POST http://$3:8091/pools/default/buckets/travel-users/collections/userData -d name=flights

# show what we have made
echo '\n\n\n\nTHE FINAL RESULT'
curl -u $1:$2 -X GET http://$3:8091/pools/default/buckets/travel-users/collections
echo '\n'
