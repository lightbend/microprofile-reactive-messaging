/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.kafka.tck.container;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.tck.container.SimpleMessage;
import org.eclipse.microprofile.reactive.messaging.tck.container.TckMessagingPuppet;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public class KafkaTckMessagingPuppet implements TckMessagingPuppet {

  private static final AtomicLong ids = new AtomicLong();

  @Inject
  private Instance<ActorSystem> system;
  @Inject
  private Instance<Materializer> materializer;

  @Override
  public void sendMessage(String topic, Message<byte[]> message) {
    ProducerSettings<byte[], byte[]> settings = ProducerSettings.create(system.get(), new ByteArraySerializer(), new ByteArraySerializer())
        .withProperty(ProducerConfig.CLIENT_ID_CONFIG, "tck-local-container-controller-sender-" + ids.incrementAndGet())
        .withBootstrapServers("localhost:9092");
    try {
      Source.single(new ProducerRecord<byte[], byte[]>(topic, message.getPayload()))
          .runWith(Producer.plainSink(settings), materializer.get())
          .toCompletableFuture().get(testEnvironment().receiveTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<Message<byte[]>> receiveMessage(String topic, Duration timeout) {
    ConsumerSettings<byte[], byte[]> settings = ConsumerSettings.create(system.get(), new ByteArrayDeserializer(), new ByteArrayDeserializer())
        .withProperty(ConsumerConfig.CLIENT_ID_CONFIG, "tck-local-container-controller-receiver-" + ids.incrementAndGet())
        .withGroupId("tck-local-container-receiver")
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        .withBootstrapServers("localhost:9092");
    try {
      Optional<Message<byte[]>> message = Consumer.committableSource(settings, Subscriptions.topics(topic))
          .idleTimeout(timeout)
          .take(1)
          .mapAsync(1, msg ->
            msg.committableOffset().commitJavadsl()
                .thenApply(done ->
                    Optional.of((Message<byte[]>) new SimpleMessage<>(msg.record().value()))
                )
          )
          .runWith(Sink.head(), materializer.get())
          .exceptionally(ex -> {
            Throwable unwrapped = ex;
            if (ex instanceof CompletionException) {
              unwrapped = ex.getCause();
            }
            if (unwrapped instanceof TimeoutException) {
              return Optional.empty();
            }
            else {
              throw new CompletionException(unwrapped);
            }
          }).toCompletableFuture().get(testEnvironment().receiveTimeout().toMillis(), TimeUnit.MILLISECONDS);

      return message;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
