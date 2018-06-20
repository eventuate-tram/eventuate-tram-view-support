#! /bin/bash

subscriber=$1
sudo docker run -t --rm --entrypoint=bin/kafka-consumer-groups.sh test-eventuateio-local-kafka --bootstrap-server "$EVENTUATELOCAL_KAFKA_BOOTSTRAP_SERVERS" --group $subscriber --describe


