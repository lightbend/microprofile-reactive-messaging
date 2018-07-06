/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.kafka.tck;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.kafka.javadsl.Producer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.tck.framework.SimpleMessage;
import org.eclipse.microprofile.reactive.messaging.tck.spi.TckMessagingPuppet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class KafkaTckLocalContainerController implements TckMessagingPuppet {

  private final ActorSystem system;
  private final Materializer materializer;
  private static final AtomicLong ids = new AtomicLong();

  @Inject
  public KafkaTckLocalContainerController(ActorSystem system, Materializer materializer) {
    this.system = system;
    this.materializer = materializer;
  }

  @Override
  public void sendMessage(String topic, Message<byte[]> message) {
    ProducerSettings<byte[], byte[]> settings = ProducerSettings.create(system, new ByteArraySerializer(), new ByteArraySerializer())
        .withProperty(ProducerConfig.CLIENT_ID_CONFIG, "tck-local-container-controller-sender-" + ids.incrementAndGet())
        .withBootstrapServers("localhost:9092");
    try {
      Source.single(new ProducerRecord<byte[], byte[]>(topic, message.getPayload()))
          .runWith(Producer.plainSink(settings), materializer)
          .toCompletableFuture().get(testEnvironment().receiveTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Optional<Message<byte[]>> receiveMessage(String topic, Duration timeout) {
    ConsumerSettings<byte[], byte[]> settings = ConsumerSettings.create(system, new ByteArrayDeserializer(), new ByteArrayDeserializer())
        .withProperty(ProducerConfig.CLIENT_ID_CONFIG, "tck-local-container-controller-receiver-" + ids.incrementAndGet())
        .withGroupId("tck-local-container-receiver")
        .withBootstrapServers("localhost:9092");
    try {
      return Consumer.committableSource(settings, Subscriptions.topics(topic))
          .idleTimeout(timeout)
          .runWith(Sink.head(), materializer)
          .thenCompose(msg ->
              msg.committableOffset().commitJavadsl()
               .thenApply(done ->
                   Optional.of((Message<byte[]>) new SimpleMessage<>(msg.record().value()))
               )
          ).exceptionally(ex -> {
            if (ex instanceof TimeoutException) {
              return Optional.empty();
            } else {
              throw new CompletionException(ex);
            }
          }).toCompletableFuture().get(testEnvironment().receiveTimeout().toMillis(), TimeUnit.MILLISECONDS);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}