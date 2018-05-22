package com.lightbend.microprofile.reactive.messaging.impl;

import com.lightbend.microprofile.reactive.messaging.spi.MessageDeserializer;
import com.lightbend.microprofile.reactive.messaging.spi.SerializationSupport;
import com.lightbend.microprofile.reactive.messaging.spi.SubscribingStream;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import javax.enterprise.inject.spi.Annotated;
import java.lang.reflect.Type;
import java.util.Optional;

class SubscribingStreamImpl<T> implements SubscribingStream<T> {

  private final SerializationSupport serializationSupport;
  private final Incoming incoming;
  private final Type messageType;
  private final Annotated annotated;
  private final Optional<Type> wrapperType;

  SubscribingStreamImpl(SerializationSupport serializationSupport, Incoming incoming, Type messageType, Annotated annotated, Optional<Type> wrapperType) {
    this.serializationSupport = serializationSupport;
    this.incoming = incoming;
    this.messageType = messageType;
    this.annotated = annotated;
    this.wrapperType = wrapperType;
  }

  @Override
  public Incoming incoming() {
    return incoming;
  }

  @Override
  public Type messageType() {
    return messageType;
  }

  @Override
  public MessageDeserializer<T> createDeserializer() {
    return serializationSupport.deserializerFor(messageType);
  }

  @Override
  public Annotated annotated() {
    return annotated;
  }

  @Override
  public Optional<Type> wrapperType() {
    return wrapperType;
  }
}
