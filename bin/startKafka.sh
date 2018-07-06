#!/bin/bash

KAFKA_INSTALL_DIR=local-kafka-install

set -e

cd "$( dirname "${BASH_SOURCE[0]}" )/.."

if [[ -d "${KAFKA_INSTALL_DIR}" ]]; then
    cd "${KAFKA_INSTALL_DIR}"
    bin/zookeeper-server-start.sh -daemon config/zookeeper.properties
    bin/kafka-server-start.sh -daemon config/server.properties
    echo "ZooKeeper and Kafka started."
else
    echo "Kafka has not been installed in ${KAFKA_INSTALL_DIR}, please run bin/installKafka.sh first."
    exit 1
fi
