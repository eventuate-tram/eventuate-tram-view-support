#! /bin/bash

set -e

docker="./gradlew ${DATABASE?}${MODE?}Compose"

${docker}Down
${docker}Up

./gradlew build

${docker}Down