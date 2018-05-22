package com.lightbend.microprofile.reactive.messaging.kafka;

import akka.Done;
import akka.NotUsed;
import akka.japi.function.Creator;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.RestartSink;
import akka.stream.javadsl.RestartSource;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.lightbend.microprofile.reactive.messaging.spi.ValidatedSubscribingStream;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.io.Closeable;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class KafkaValidatedSubscribingStream<K, T> implements ValidatedSubscribingStream<T> {

  private static final long MIN_BACKOFF_MS = 3000;
  private static final long MAX_BACKOFF_MS = 30000;
  private static final double RANDOM_FACTOR = 0.2;
  private static final int MAX_RESTARTS = -1;
  private static final int ACK_PARALLELISM = 1;

  private final AtomicLong ids = new AtomicLong();

  private final Materializer materializer;
  private final ConsumerSettings<K, T> consumerSettings;
  private final String topic;
  private final Optional<KafkaInstanceProvider> kafkaInstanceProvider;
  private final String clientId;

  public KafkaValidatedSubscribingStream(Materializer materializer, ConsumerSettings<K, T> consumerSettings, String topic,
      Optional<KafkaInstanceProvider> instanceProvider, String clientId) {
    this.materializer = materializer;
    this.consumerSettings = consumerSettings;
    this.topic = topic;
    this.kafkaInstanceProvider = instanceProvider;
    this.clientId = clientId;
  }

  @Override
  public Closeable runFlow(Creator<Flow<Message<T>, Message<?>, NotUsed>> createConsumer) {
    ControlCloseable closeable = new ControlCloseable();

    RestartSource.onFailuresWithBackoff(
        Duration.ofMillis(MIN_BACKOFF_MS),
        Duration.ofMillis(MAX_BACKOFF_MS),
        RANDOM_FACTOR,
        MAX_RESTARTS,
        () -> {
          Flow<KafkaConsumerMessage<K, T>, Message<?>, NotUsed> consumer = (Flow) createConsumer.create();

          return getCommittableSource(closeable)
              .via(consumer)
              .mapAsync(ACK_PARALLELISM, message ->
                  message.ack().thenApply(v -> Done.getInstance())
              );
        }
    ).runWith(Sink.ignore(), materializer);

    return closeable;
  }

  @Override
  public Closeable runSink(Creator<Sink<Message<T>, NotUsed>> createConsumer) {
    ControlCloseable closeable = new ControlCloseable();

    Source<KafkaConsumerMessage<K, T>, NotUsed> source = RestartSource.onFailuresWithBackoff(
        Duration.ofMillis(MIN_BACKOFF_MS),
        Duration.ofMillis(MAX_BACKOFF_MS),
        RANDOM_FACTOR,
        MAX_RESTARTS,
        () -> getCommittableSource(closeable)
    );

    Sink<KafkaConsumerMessage<K, T>, NotUsed> sink = RestartSink.withBackoff(
        Duration.ofMillis(MIN_BACKOFF_MS),
        Duration.ofMillis(MAX_BACKOFF_MS),
        RANDOM_FACTOR,
        MAX_RESTARTS,
        () -> (Sink) createConsumer.create()
    );

    source.runWith(sink, materializer);

    return closeable;
  }

  private Source<KafkaConsumerMessage<K, T>, NotUsed> getCommittableSource(ControlCloseable closeable) {
    if (consumerSettings.properties().contains(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)) {
      return getCommittableSource(closeable, consumerSettings);
    } else if (kafkaInstanceProvider.isPresent()) {
      return Source.fromSourceCompletionStage(
          kafkaInstanceProvider.get().bootstrapServers()
              .thenApply(bootstrapServers ->
                  getCommittableSource(closeable, consumerSettings.withBootstrapServers(bootstrapServers)))
      ).mapMaterializedValue(cs -> NotUsed.getInstance());
    } else {
      // todo better default, probably read from config
      return getCommittableSource(closeable, consumerSettings.withBootstrapServers("localhost:9092"));
    }
  }

  private Source<KafkaConsumerMessage<K, T>, NotUsed> getCommittableSource(ControlCloseable closeable, ConsumerSettings<K, T> settings) {
    return Consumer.committableSource(settings.withProperty(ConsumerConfig.CLIENT_ID_CONFIG, clientId + "-" + ids.incrementAndGet()),
        Subscriptions.topics(topic))
        .<KafkaConsumerMessage<K, T>>map(KafkaConsumerMessageImpl::new)
        .mapMaterializedValue(control -> {
          closeable.set(control);
          return NotUsed.getInstance();
        });
  }

  private static class ControlCloseable implements Closeable {
    private AtomicReference<Object> closedOrControl = new AtomicReference<>();

    @Override
    public void close() {
      Object old = closedOrControl.getAndSet(Closed.INSTANCE);
      if (old instanceof Consumer.Control) {
        ((Consumer.Control) old).shutdown();
      }
    }

    void set(Consumer.Control control) {
      Object old;
      do {
        old = closedOrControl.get();
        if (old == Closed.INSTANCE) {
          control.shutdown();
          break;
        }
      } while (!closedOrControl.compareAndSet(old, control));
    }

    private enum Closed {
      INSTANCE
    }
  }
}
