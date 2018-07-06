#!/bin/bash

# This should be set to the commit hash that is being tracked.
MP_REACTIVE_COMMIT="60f67ef"
# To track a particular pull request, put it's number here, otherwise comment it out.
MP_REACTIVE_PR="64"

LIGHTBEND_MP_STREAMS_COMMIT="51becc8"
#LIGHTBEND_MP_STREAMS_PR=""

set -e

cd "$( dirname "${BASH_SOURCE[0]}" )/.."
mkdir -p target
cd target

if [[ -d microprofile-reactive ]]; then
    cd microprofile-reactive
    git fetch
else
    git clone https://github.com/eclipse/microprofile-reactive.git
    cd microprofile-reactive
fi

if [[ -n ${MP_REACTIVE_PR+x} ]]; then
    git fetch origin "pull/${MP_REACTIVE_PR}/head"
fi

git checkout "${MP_REACTIVE_COMMIT}"

mvn clean install -Dmaven.test.skip -Drat.skip=true -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -Dasciidoctor.skip=true

cd ..

if [[ -d lightbend-microprofile-reactive-streams ]]; then
    cd lightbend-microprofile-reactive-streams
    git fetch
else
    git clone https://github.com/lightbend/microprofile-reactive-streams.git lightbend-microprofile-reactive-streams
    cd lightbend-microprofile-reactive-streams
fi

if [[ -n ${LIGHTBEND_MP_STREAMS_PR+x} ]]; then
    git fetch origin "pull/${LIGHTBEND_MP_STREAMS_PR}/head"
fi

git checkout "${LIGHTBEND_MP_STREAMS_COMMIT}"

mvn clean install -Dmaven.test.skip -Drat.skip=true -Dcheckstyle.skip=true -Dmaven.javadoc.skip=true -Dasciidoctor.skip=true
