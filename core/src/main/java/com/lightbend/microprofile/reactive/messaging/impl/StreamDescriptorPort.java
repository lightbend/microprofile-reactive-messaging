/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.impl;

import org.eclipse.microprofile.reactive.messaging.Message;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;

class StreamDescriptorPort<T extends Annotation> {
  private final T annotation;
  private final Type messageType;
  private final Optional<Type> wrapperType;

  public StreamDescriptorPort(T annotation, Type messageType, Optional<Type> wrapperType) {
    this.annotation = annotation;
    this.messageType = messageType;
    this.wrapperType = wrapperType;
  }

  public T getAnnotation() {
    return annotation;
  }

  public Type getMessageType() {
    return messageType;
  }

  public Optional<Type> getWrapperType() {
    return wrapperType;
  }
}
