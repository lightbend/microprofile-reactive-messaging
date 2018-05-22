package com.lightbend.microprofile.reactive.messaging.spi;

/**
 * A message serializer.
 *
 * This will be moved in some form to the messaging spec, it exists here now as a placeholder.
 *
 * @param <T>
 */
public interface MessageSerializer<T> {
  byte[] toBytes(T message);
}
