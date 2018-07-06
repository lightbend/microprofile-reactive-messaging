/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.impl;

import akka.Done;
import akka.NotUsed;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import com.lightbend.microprofile.reactive.messaging.spi.ValidatedSubscribingStream;
import com.lightbend.microprofile.reactive.streams.akka.AkkaEngine;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.io.Closeable;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

class ValidatedSubscribingStreamRunner<T> implements StreamRunner {

  private static final int SINGLE_METHOD_PARALLELISM = 1;
  private static final int AUTO_ACK_PARALLELISM = 4;

  private final AkkaEngine akkaEngine;
  private final ValidatedSubscribingStream<T> subscribingStream;
  private final StreamDescriptor descriptor;

  ValidatedSubscribingStreamRunner(AkkaEngine akkaEngine, ValidatedSubscribingStream<T> subscribingStream,
      StreamDescriptor descriptor) {
    this.akkaEngine = akkaEngine;
    this.subscribingStream = subscribingStream;
    this.descriptor = descriptor;
  }

  public Closeable run(Object bean) {
    switch (descriptor.getShape()) {
      case PROCESSOR_BUILDER:
      case RS_PROCESSOR:
      case AKKA_FLOW:
      case ASYNCHRONOUS_METHOD:
      case SYNCHRONOUS_METHOD:
        return subscribingStream.runFlow(() -> createFlow(bean));
      case SUBSCRIBER_BUILDER:
      case RS_SUBSCRIBER:
      case AKKA_SINK:
        return subscribingStream.runSink(() -> createSink(bean));
      default:
        throw new RuntimeException("Invalid shape for subscriber: " + descriptor.getShape());
    }
  }

  private Flow<Message<T>, Message<?>, NotUsed> createFlow(Object bean) throws Exception {

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
        rawFlow = Flow.create().mapAsync(SINGLE_METHOD_PARALLELISM, t ->
          ((CompletionStage) invokeMethod(bean, t)).thenApply(r -> {
            if (r == null) {
              return Done.getInstance();
            } else {
              return r;
            }
          })
        );
        break;
      case SYNCHRONOUS_METHOD:
        rawFlow = Flow.create().map(t -> {
          Object result = invokeMethod(bean, t);
          if (result == null) {
            return Done.getInstance();
          } else {
            return result;
          }
        });
        break;
      default:
        throw new RuntimeException("Invalid shape for flow subscriber: " + descriptor.getShape());
    }

    if (wrappedIncoming() && descriptor.isIncomingDestinationWrapped()) {
      return ((Flow<Message<T>, Message<?>, ?>) rawFlow).mapMaterializedValue(v -> NotUsed.getInstance());
    } else {

      Flow<Message<T>, Message<?>, ?> flow;
      if (wrappedIncoming()) {
        return (Flow) ((Flow<Message<T>, ?, ?>) rawFlow)
            .map(Message::of);
      } else if (descriptor.isIncomingDestinationWrapped()) {
        flow = Flow.<Message<T>>create()
            .map(Message::getPayload)
            .via((Flow<T, Message<?>, ?>) rawFlow);
      } else {
        flow = Flow.<Message<T>>create()
            .map(Message::getPayload)
            .via((Flow<T, ?, ?>) rawFlow)
            .map(Message::of);
      }

      return FlowUtils.bypassFlow((Flow) flow);
    }
  }

  private Sink<Message<T>, NotUsed> createSink(Object bean) throws Exception {

    Sink rawSink;
    switch(descriptor.getShape()) {
      case SUBSCRIBER_BUILDER:
        rawSink = akkaEngine.buildSink(invokeMethod(bean));
        break;
      case RS_SUBSCRIBER:
        rawSink = Sink.fromSubscriber(invokeMethod(bean));
        break;
      case AKKA_SINK:
        rawSink = invokeMethod(bean);
        break;
      default:
        throw new RuntimeException("Invalid shape for sink subscriber: " + descriptor.getShape());
    }

    if (wrappedIncoming()) {
      return ((Sink<Message<T>, ?>) rawSink).mapMaterializedValue(v -> NotUsed.getInstance());
    } else {
      return Flow.<Message<T>>create()
          .mapAsync(AUTO_ACK_PARALLELISM, message -> message.ack().thenApply(v -> message.getPayload()))
          .to(rawSink);
    }
  }

  private boolean wrappedIncoming() {
    return descriptor.getIncoming().get().getWrapperType().isPresent();
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
