/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.spi;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * Returned by a {@link LightbendMessagingProvider} to get a subscriber to consume messages to be published.
 */
public interface ValidatedPublishingStream<T> {
  /**
   * Get the consumer for this stream.
   */
  Flow<Message<T>, Message<?>, NotUsed> getFlowConsumer();
}
