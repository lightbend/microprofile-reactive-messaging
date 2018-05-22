package com.lightbend.microprofile.reactive.messaging.kafka;

import org.eclipse.microprofile.reactive.messaging.Message;

public interface KafkaConsumerMessage<K, T> extends Message<T> {
  long getOffset();
  K getKey();
}
