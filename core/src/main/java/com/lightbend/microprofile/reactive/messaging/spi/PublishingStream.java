package com.lightbend.microprofile.reactive.messaging.spi;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.inject.spi.Annotated;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Descriptor for a publishing stream.
 */
public interface PublishingStream<T> {

  /**
   * The outgoing annotation.
   */
  Outgoing outgoing();

  /**
   * The type of message being subscribed to - ie T.
   */
  Type messageType();

  /**
   * The wrapper type of the message, if it's wrapped.
   *
   * This allows message providers to subtype {@link org.eclipse.microprofile.reactive.messaging.Message} for
   * the purpose of allowing users to attach extra meta data to messages that they are publishing.
   *
   * This must be a subtype of {@link org.eclipse.microprofile.reactive.messaging.Message}.
   *
   * The messaging provider must validate that it supports publishing messages of this type.
   */
  Optional<Type> wrapperType();

  /**
   * Invoke if a serializer is needed.
   *
   * Some providers handle message serialization internally, the configured serializer only needs to be used if not.
   */
  MessageSerializer<T> createSerializer();

  /**
   * The annotated element that represents the stream, allows the provider to read additional meta-data from it.
   */
  Annotated annotated();
}