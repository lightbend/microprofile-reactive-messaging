#!/bin/bash

KAFKA_VERSION=1.1.0
KAFKA_INSTALL_DIR=local-kafka-install

set -e

cd "$( dirname "${BASH_SOURCE[0]}" )/.."
if [[ -d ${KAFKA_INSTALL_DIR} ]]; then
    echo "Kafka already installed."
else
    mkdir -p ${KAFKA_INSTALL_DIR}
    cd ${KAFKA_INSTALL_DIR}
    wget http://www.us.apache.org/dist/kafka/${KAFKA_VERSION}/kafka_2.11-${KAFKA_VERSION}.tgz -O kafka.tgz
    tar xzf kafka.tgz --strip-components 1
fi
