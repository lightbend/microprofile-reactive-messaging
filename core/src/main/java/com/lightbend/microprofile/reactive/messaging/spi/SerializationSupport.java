package com.lightbend.microprofile.reactive.messaging.spi;

import java.lang.reflect.Type;

public interface SerializationSupport {
    <T> MessageSerializer<T> serializerFor(Type type);
    <T> MessageDeserializer<T> deserializerFor(Type type);
}
