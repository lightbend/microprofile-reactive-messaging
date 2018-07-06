#!/bin/bash

KAFKA_INSTALL_DIR=local-kafka-install

set -e

cd "$( dirname "${BASH_SOURCE[0]}" )/.."

if [[ -d "${KAFKA_INSTALL_DIR}" ]]; then
    cd "${KAFKA_INSTALL_DIR}"
    bin/kafka-server-stop.sh
    bin/zookeeper-server-stop.sh
else
    echo "Kafka has not been installed in ${KAFKA_INSTALL_DIR}, please run bin/installKafka.sh first."
    exit 1
fi
