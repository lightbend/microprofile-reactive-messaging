/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.impl;

import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import com.lightbend.microprofile.reactive.messaging.spi.ValidatedPublishingStream;
import com.lightbend.microprofile.reactive.messaging.spi.ValidatedSubscribingStream;
import com.lightbend.microprofile.reactive.streams.akka.AkkaEngine;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletionStage;

class ValidatedProcessingStreamRunner<T, R> implements StreamRunner {

  private static final int SINGLE_METHOD_PARALLELISM = 1;

  private final AkkaEngine akkaEngine;
  private final ValidatedSubscribingStream<T> subscribingStream;
  private final ValidatedPublishingStream<R> publishingStream;
  private final StreamDescriptor descriptor;


  ValidatedProcessingStreamRunner(AkkaEngine akkaEngine, ValidatedSubscribingStream<T> subscribingStream,
      ValidatedPublishingStream<R> publishingStream, StreamDescriptor descriptor) {
    this.akkaEngine = akkaEngine;
    this.subscribingStream = subscribingStream;
    this.publishingStream = publishingStream;
    this.descriptor = descriptor;
  }

  public Closeable run(Object bean) {
    return subscribingStream.runFlow(() -> create(bean));
  }

  private Flow<Message<T>, Message<?>, NotUsed> create(Object bean) throws Exception {

    Flow rawFlow;
    switch(descriptor.getShape()) {
      case PROCESSOR_BUILDER:
        rawFlow = akkaEngine.buildFlow(invokeMethod(bean));
        break;
      case RS_PROCESSOR:
        rawFlow = Flow.fromProcessor(() -> invokeMethod(bean));
        break;
      case AKKA_FLOW:
        rawFlow = invokeMethod(bean);
        break;
      case ASYNCHRONOUS_METHOD:
        rawFlow = Flow.create().mapAsync(SINGLE_METHOD_PARALLELISM, t -> (CompletionStage) invokeMethod(bean, t));
        break;
      case SYNCHRONOUS_METHOD:
        rawFlow = Flow.create().map(t -> invokeMethod(bean, t));
        break;
      default:
        throw new RuntimeException("Invalid shape for processor: " + descriptor.getShape());
    }

    Flow<Message<R>, Message<?>, NotUsed> consumer = publishingStream.getFlowConsumer();

    if (wrappedIncoming() && wrappedOutgoing()) {
      return ((Flow<Message<T>, Message<R>, ?>) rawFlow).viaMat(consumer, Keep.right());
    } else {

      Flow<Message<T>, Message<R>, ?> flow;
      if (wrappedIncoming()) {
        flow = ((Flow<Message<T>, R, ?>) rawFlow)
            .map(Message::of);
      } else if (wrappedOutgoing()) {
        flow = Flow.<Message<T>>create()
            .map(Message::getPayload)
            .via((Flow<T, Message<R>, ?>) rawFlow);
      } else {
        flow = Flow.<Message<T>>create()
            .map(Message::getPayload)
            .via((Flow<T, R, ?>) rawFlow)
            .map(Message::of);
      }

      return FlowUtils.bypassFlow(flow).viaMat(consumer, Keep.right());
    }
  }

  private boolean wrappedIncoming() {
    return descriptor.getIncoming().get().getWrapperType().isPresent();
  }

  private boolean wrappedOutgoing() {
    return descriptor.getOutgoing().get().getWrapperType().isPresent();
  }

  private <S> S invokeMethod(Object bean, Object... args) throws Exception {
    try {
      return (S) descriptor.getAnnotated().getJavaMember().invoke(bean, args);
    } catch (InvocationTargetException ite) {
      if (ite.getCause() instanceof Exception) {
        throw (Exception) ite.getCause();
      } else if (ite.getCause() instanceof Error) {
        throw (Error) ite.getCause();
      } else {
        throw ite;
      }
    }
  }
}
