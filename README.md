# Lightbend MicroProfile Reactive Messaging

This provides an implementation of MicroProfile Reactive Messaging built on Akka Streams and Alpakka.

## Developing

Currently due to the state of very active of the specs it depends on, it depends on snapshot versions of a number of things. You can use the `bin/buildDeps.sh` script to checkout, build and install the dependencies locally. The `buildDeps.sh` script hardcodes the commit hash of the repo that it depends on, so if you want to upgrade, you must update the commit hashes in that script.

## Kafka support

Currently, Kafka is supported as a messaging provider.

To work with it or run the tests, you'll need a copy of Kafka installed. Convenience scripts exist to download, install and run Kafka exist in the `bin` directory, including `installKafka.sh`, `startKafka.sh` and `runKafka.sh`. These install kafka into the root directory of this repo, in a directory called `local-kafka-install`.

The tests for the Kafka support have their own artifact, this is so that those tests can be depended upon (in a profile) by the MicroProfile Reactive Messaging TCK itself, to allow for faster development of the TCK.
