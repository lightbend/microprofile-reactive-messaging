package com.lightbend.microprofile.reactive.messaging.impl;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import java.util.Optional;

class StreamDescriptor {
  private final AnnotatedType beanType;
  private final AnnotatedMethod annotated;
  private final Optional<StreamDescriptorPort<Incoming>> incoming;
  private final Optional<StreamDescriptorPort<Outgoing>> outgoing;
  private final StreamShape shape;
  private final boolean incomingDestinationWrapped;

  public StreamDescriptor(AnnotatedType beanType, AnnotatedMethod annotated, Optional<StreamDescriptorPort<Incoming>> incoming, Optional<StreamDescriptorPort<Outgoing>> outgoing, StreamShape shape, boolean incomingDestinationWrapped) {
    this.beanType = beanType;
    this.annotated = annotated;
    this.incoming = incoming;
    this.outgoing = outgoing;
    this.shape = shape;
    this.incomingDestinationWrapped = incomingDestinationWrapped;
  }

  public AnnotatedType getBeanType() {
    return beanType;
  }

  public AnnotatedMethod getAnnotated() {
    return annotated;
  }

  public Optional<StreamDescriptorPort<Incoming>> getIncoming() {
    return incoming;
  }

  public Optional<StreamDescriptorPort<Outgoing>> getOutgoing() {
    return outgoing;
  }

  public StreamShape getShape() {
    return shape;
  }

  /**
   * This returns true if there is no outgoing destination, but the stream shape is a processor/flow and the output type
   * is a subclass of Message.
   */
  public boolean isIncomingDestinationWrapped() {
    return incomingDestinationWrapped;
  }
}
