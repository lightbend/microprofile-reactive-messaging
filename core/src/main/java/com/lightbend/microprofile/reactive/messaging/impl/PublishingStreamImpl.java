/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.impl;

import com.lightbend.microprofile.reactive.messaging.spi.MessageSerializer;
import com.lightbend.microprofile.reactive.messaging.spi.PublishingStream;
import com.lightbend.microprofile.reactive.messaging.spi.SerializationSupport;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.inject.spi.Annotated;
import java.lang.reflect.Type;
import java.util.Optional;

class PublishingStreamImpl<T> implements PublishingStream<T> {

  private final SerializationSupport serializationSupport;
  private final Outgoing outgoing;
  private final Type messageType;
  private final Annotated annotated;
  private final Optional<Type> wrapperType;

  PublishingStreamImpl(SerializationSupport serializationSupport, Outgoing outgoing, Type messageType, Annotated annotated, Optional<Type> wrapperType) {
    this.serializationSupport = serializationSupport;
    this.outgoing = outgoing;
    this.messageType = messageType;
    this.annotated = annotated;
    this.wrapperType = wrapperType;
  }

  @Override
  public Outgoing outgoing() {
    return outgoing;
  }

  @Override
  public Type messageType() {
    return messageType;
  }

  @Override
  public MessageSerializer<T> createSerializer() {
    return serializationSupport.serializerFor(messageType);
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
