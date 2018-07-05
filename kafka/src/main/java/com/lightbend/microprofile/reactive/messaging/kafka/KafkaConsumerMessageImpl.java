package com.lightbend.microprofile.reactive.messaging.kafka;

import akka.kafka.ConsumerMessage;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.concurrent.CompletionStage;

class KafkaConsumerMessageImpl<K, T> implements KafkaConsumerMessage<K, T> {
  private final ConsumerMessage.CommittableMessage<K, T> message;

  KafkaConsumerMessageImpl(ConsumerMessage.CommittableMessage<K, T> message) {
    this.message = message;
  }

  @Override
  public K getKey() {
    return message.record().key();
  }

  @Override
  public long getOffset() {
    return message.record().offset();
  }

  @Override
  public T getPayload() {
    return message.record().value();
  }

  @Override
  public CompletionStage<Void> ack() {
    return message.committableOffset().commitJavadsl().thenApply(d -> null);
  }

  @Override
  public String toString() {
    return "KafkaConsumerMessage{" +
        "message=" + message +
        '}';
  }
}
