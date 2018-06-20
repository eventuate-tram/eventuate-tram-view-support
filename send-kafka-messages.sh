#! /bin/bash

topic=$1
messages=$2

echo "$messages" | docker run -i --rm --entrypoint=bin/kafka-console-producer.sh test-eventuateio-local-kafka --broker-list "$EVENTUATELOCAL_KAFKA_BOOTSTRAP_SERVERS" --topic $topic --property "message.separator=:"
