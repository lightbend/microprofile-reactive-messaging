/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.spi;

import akka.NotUsed;
import akka.japi.function.Creator;
import akka.stream.KillSwitch;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.io.Closeable;

/**
 * Returned by a {@link LightbendMessagingProvider} to allow the stream to be run.
 */
public interface ValidatedSubscribingStream<T> {

  /**
   * Run the stream with the given flow.
   *
   * It is the responsibility of this method to run the subscriber and keep it running when failures occur. It's also
   * the responsibility of this method to acknowledge the outgoing messages from this stream. They may or may not be
   * correlated with the incoming messages.
   *
   * When the kill switch is shutdown, the stream must be shut down.
   */
  Closeable runFlow(Creator<Flow<Message<T>, Message<?>, NotUsed>> createConsumer);

  /**
   * Run the stream with the given sink.
   */
  Closeable runSink(Creator<Sink<Message<T>, NotUsed>> createConsumer);
}
