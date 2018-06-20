#! /bin/bash

topic=$1

sudo docker run -t --rm --entrypoint=bin/kafka-topics.sh test-eventuateio-local-kafka --zookeeper "$EVENTUATELOCAL_ZOOKEEPER_CONNECTION_STRING" --create --replication-factor 1 --partitions 1 --topic $topic
