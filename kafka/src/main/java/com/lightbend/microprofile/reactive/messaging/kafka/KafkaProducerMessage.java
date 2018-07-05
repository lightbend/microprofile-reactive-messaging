package com.lightbend.microprofile.reactive.messaging.kafka;

import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class KafkaProducerMessage<K, T> implements Message<T> {
  private final K key;
  private final T payload;
  private final Supplier<CompletionStage<Void>> ack;

  public KafkaProducerMessage(K key, T payload) {
    this(key, payload, () -> CompletableFuture.completedFuture(null));
  }

  public KafkaProducerMessage(T payload) {
    this(null, payload);
  }

  public KafkaProducerMessage(K key, T payload, Supplier<CompletionStage<Void>> ack) {
    this.key = key;
    this.payload = payload;
    this.ack = ack;
  }

  public KafkaProducerMessage(T payload, Supplier<CompletionStage<Void>> ack) {
    this(null, payload, ack);
  }

  public K getKey() {
    return key;
  }

  public T getPayload() {
    return payload;
  }

  @Override
  public CompletionStage<Void> ack() {
    return ack.get();
  }

  @Override
  public String toString() {
    return "KafkaProducerMessage{" +
        "key=" + key +
        ", payload=" + payload +
        ", ack=" + ack +
        '}';
  }
}
