#!/bin/bash -e

docker run -d --name mongodb -p 27017:27017 nordlys-mongodb
docker run -d --name els-nordlys -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" nordlys-elasticsearch

echo "Done ;)"

