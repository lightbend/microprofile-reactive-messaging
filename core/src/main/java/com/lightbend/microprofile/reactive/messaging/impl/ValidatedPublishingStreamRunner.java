/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.impl;

import akka.Done;
import akka.NotUsed;
import akka.stream.KillSwitch;
import akka.stream.KillSwitches;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RestartSource;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.lightbend.microprofile.reactive.messaging.spi.ValidatedPublishingStream;
import com.lightbend.microprofile.reactive.streams.akka.AkkaEngine;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.reactivestreams.Publisher;

import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

class ValidatedPublishingStreamRunner<T> implements StreamRunner {

  private final Materializer materializer;
  private final AkkaEngine akkaEngine;
  private final ValidatedPublishingStream<T> publishingStream;
  private final StreamDescriptor descriptor;

  ValidatedPublishingStreamRunner(Materializer materializer, AkkaEngine akkaEngine, ValidatedPublishingStream<T> publishingStream,
      StreamDescriptor descriptor) {
    this.materializer = materializer;
    this.akkaEngine = akkaEngine;
    this.publishingStream = publishingStream;
    this.descriptor = descriptor;
  }

  private static final long MIN_BACKOFF_MS = 3000;
  private static final long MAX_BACKOFF_MS = 30000;
  private static final double RANDOM_FACTOR = 0.2;
  private static final int MAX_RESTARTS = -1;
  private static final int ACK_PARALLELISM = 1;

  public Closeable run(Object bean) {

    Source<Done, NotUsed> restartSource = RestartSource.onFailuresWithBackoff(
        Duration.ofMillis(MIN_BACKOFF_MS),
        Duration.ofMillis(MAX_BACKOFF_MS),
        RANDOM_FACTOR,
        MAX_RESTARTS,
        () -> {
          Object stream = descriptor.getAnnotated().getJavaMember().invoke(bean);
          Source rawSource;
          switch (descriptor.getShape()) {
            case PUBLISHER_BUILDER:
              rawSource = akkaEngine.buildSource((PublisherBuilder) stream);
              break;
            case RS_PUBLISHER:
              rawSource = Source.fromPublisher((Publisher) stream);
              break;
            case AKKA_SOURCE:
              rawSource = (Source) stream;
              break;
            default:
              throw new RuntimeException("Invalid shape for publisher: " + descriptor.getShape());
          }

          Flow<Message<T>, Message<?>, NotUsed> consumer = publishingStream.getFlowConsumer();

          if (descriptor.getOutgoing().get().getWrapperType().isPresent()) {
            Source<Message<T>, ?> source = rawSource;
            return source.via(consumer)
                .mapAsync(ACK_PARALLELISM, m ->
                    m.ack().thenApply(v -> Done.getInstance()));
          } else {
            // Since the published messages don't need to be acked, we just wrap them in a dummy message, and don't worry about
            // acking it.
            return rawSource.map(payload -> Message.of(payload))
                .via(consumer)
                .map(m -> Done.getInstance());
          }
        }
    );

    KillSwitch killSwitch = restartSource.viaMat(KillSwitches.single(), Keep.right())
        .to(Sink.ignore())
        .run(materializer);

    return killSwitch::shutdown;
  }

}
