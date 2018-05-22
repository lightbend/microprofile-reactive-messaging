package com.lightbend.microprofile.reactive.messaging.spi;

/**
 * A message deserializer.
 *
 * This will be moved in some form to the messaging spec, it exists here now as a placeholder.
 *
 * @param <T>
 */
public interface MessageDeserializer<T> {
  T fromBytes(byte[] bytes);
}
