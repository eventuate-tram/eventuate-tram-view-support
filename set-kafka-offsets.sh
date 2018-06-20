#! /bin/bash

subscriber=$1

echo $subscriber

shift
for i in "$@"
do
    set -- $i
    IFS=":"; declare -a topic_partition_offset=($*)

    topic="${topic_partition_offset[0]}"
    partition="${topic_partition_offset[1]}"
    offset="${topic_partition_offset[2]}"

    docker run -t --rm --entrypoint=bin/kafka-consumer-groups.sh test-eventuateio-local-kafka --bootstrap-server "$EVENTUATELOCAL_KAFKA_BOOTSTRAP_SERVERS" --group $subscriber --reset-offsets --topic $topic:$partition --to-offset $offset --execute
done


