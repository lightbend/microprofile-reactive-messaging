/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.kafka;

import java.util.concurrent.CompletionStage;

public interface KafkaInstanceProvider {
  CompletionStage<String> bootstrapServers();
}
