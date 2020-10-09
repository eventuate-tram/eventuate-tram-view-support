#! /bin/bash

set -e

docker="./gradlew ${DATABASE?}${MODE?}Compose"

${docker}Down
${docker}Up

./gradlew build

export USE_DB_ID=true
export EVENTUATE_OUTBOX_ID=1

${docker}Down