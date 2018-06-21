#! /bin/bash

set -e

. ./set-env-${DATABASE?}.sh

docker-compose -f docker-compose-${DATABASE?}.yml down -v
docker-compose -f docker-compose-${DATABASE?}.yml up -d --build

./wait-for-${DATABASE?}.sh

./gradlew build

docker-compose -f docker-compose-${DATABASE?}.yml down -v