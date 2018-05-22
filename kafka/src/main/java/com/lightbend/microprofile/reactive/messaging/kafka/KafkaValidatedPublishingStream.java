package com.lightbend.microprofile.reactive.messaging.kafka;

import akka.NotUsed;
import akka.kafka.ProducerMessage;
import akka.kafka.ProducerSettings;
import akka.kafka.javadsl.Producer;
import akka.stream.FlowShape;
import akka.stream.Graph;
import akka.stream.javadsl.Flow;
import com.lightbend.microprofile.reactive.messaging.impl.FlowUtils;
import com.lightbend.microprofile.reactive.messaging.spi.ValidatedPublishingStream;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;

class KafkaValidatedPublishingStream<T> implements ValidatedPublishingStream<T> {

  private final AtomicLong ids = new AtomicLong();
  private final String topic;
  private final ProducerSettings<?, T> producerSettings;
  private final Optional<KafkaInstanceProvider> kafkaInstanceProvider;
  private final String clientId;

  KafkaValidatedPublishingStream(String topic, ProducerSettings<?, T> producerSettings, Optional<KafkaInstanceProvider> kafkaInstanceProvider,
      String clientId) {
    this.topic = topic;
    this.producerSettings = producerSettings;
    this.kafkaInstanceProvider = kafkaInstanceProvider;
    this.clientId = clientId;
  }

  @Override
  public Flow<Message<T>, Message<?>, NotUsed> getFlowConsumer() {
    ProducerSettings<Object, T> castSettings = (ProducerSettings) producerSettings;

    if (producerSettings.properties().contains(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)) {
      return getFlowConsumer(castSettings);
    } else if (kafkaInstanceProvider.isPresent()) {
      CompletionStage<Graph<FlowShape<Message<T>, Message<?>>, NotUsed>> asyncFlow =
          kafkaInstanceProvider.get().bootstrapServers()
              .thenApply(bootstrapServers -> getFlowConsumer(castSettings.withBootstrapServers(bootstrapServers)));
      return FlowUtils.asynchronouslyProvidedFlow(asyncFlow);
    } else {
      // todo better default, probably read from config
      return getFlowConsumer(castSettings.withBootstrapServers("localhost:9092"));
    }
  }

  private Flow<Message<T>, Message<?>, NotUsed> getFlowConsumer(ProducerSettings<Object, T> settings) {
    return Flow.<Message<T>>create()
        .map(message -> {
          if (message instanceof KafkaProducerMessage) {
            return new ProducerMessage.Message<>(
                new ProducerRecord<>(topic, ((KafkaProducerMessage) message).getKey(), message.getPayload()),
                message
            );
          }
          else {
            return new ProducerMessage.Message<>(
                new ProducerRecord<>(topic, message.getPayload()),
                message
            );
          }
        }).via(Producer.flow(settings.withProperty(ProducerConfig.CLIENT_ID_CONFIG, clientId + "-" + ids.incrementAndGet())))
        .map(result -> result.message().passThrough());
  }

}
