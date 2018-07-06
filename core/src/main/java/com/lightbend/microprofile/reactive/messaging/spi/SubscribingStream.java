/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.spi;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.enterprise.inject.spi.Annotated;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Descriptor for a subscribing stream.
 */
public interface SubscribingStream<T> {

  /**
   * The incoming annotation
   */
  Incoming incoming();

  /**
   * The type of message being subscribed to - ie T.
   */
  Type messageType();

  /**
   * The wrapper type of the message, if it's wrapped.
   *
   * This allows message providers to subtype {@link org.eclipse.microprofile.reactive.messaging.Message} for
   * the purpose of allowing users to read extra meta data to messages that they are subscribing to.
   *
   * This must be a subtype of {@link org.eclipse.microprofile.reactive.messaging.Message}.
   *
   * The messaging provider must validate that it supports subscribing to messages of this type, and, only emit
   * messages of this type to the stream.
   */
  Optional<Type> wrapperType();

  /**
   * Invoke if a deserializer is needed.
   *
   * Some brokers handle message deserialization internally, the configured deserializer only needs to be used if not.
   */
  MessageDeserializer<T> createDeserializer();

  /**
   * The annotated element that represents the stream, allows the provider to read additional meta-data from it.
   */
  Annotated annotated();
}
