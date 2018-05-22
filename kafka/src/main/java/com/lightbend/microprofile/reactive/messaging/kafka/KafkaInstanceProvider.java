package com.lightbend.microprofile.reactive.messaging.kafka;

import java.util.concurrent.CompletionStage;

public interface KafkaInstanceProvider {
  CompletionStage<String> bootstrapServers();
}
