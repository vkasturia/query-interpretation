#!/bin/bash -e

git submodule update --init --recursive

cd third-party/nordlys
pip install --user -r requirements.txt

cd data/raw-data/dbpedia-2015-10/type2entity-mapping/
wget http://iai.group/downloads/nordlys-v02/dbpedia-2015-10-type_to_entity.csv.bz2
cd ../../../..

docker run -d --name mongodb -p 27017:27017 -v ${PWD}:/nordlys -v ${PWD}/data:/nordlys/data mongo:4.1
docker run -d --name els-nordlys -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" elasticsearch:6.8.1

docker exec mongodb bash -c "apt update && apt install -y wget bzip2"
docker exec mongodb bash -c "cd nordlys && ./scripts/load_mongo_dumps.sh mongo_dbpedia-2015-10.tar.bz2"
docker exec mongodb bash -c "cd nordlys && ./scripts/load_mongo_dumps.sh mongo_surface_forms_dbpedia.tar.bz2"
docker exec mongodb bash -c "cd nordlys && ./scripts/load_mongo_dumps.sh mongo_surface_forms_facc.tar.bz2"
docker exec mongodb bash -c "cd nordlys && ./scripts/load_mongo_dumps.sh mongo_fb2dbp-2015-10.tar.bz2"
docker exec mongodb bash -c "cd nordlys && ./scripts/load_mongo_dumps.sh mongo_word2vec-googlenews.tar.bz2"

./scripts/build_indices.sh dbpedia
./scripts/build_indices.sh types
./scripts/build_indices.sh dbpedia_uri

#make snapshot of mongodb container as nordlys-mongodb
docker commit mongodb nordlys-mongodb
#remove mongodb container
docker rm -f mongodb

#make snapshot of elasticsearch container as nordlys-elasticsearch
docker commit els-nordlys nordlys-elasticsearch
#remove mongodb container
docker rm -f els-nordlys

echo "Done ;)"

