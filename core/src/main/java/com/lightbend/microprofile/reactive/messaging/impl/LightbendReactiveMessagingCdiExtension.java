/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.impl;

import akka.japi.Pair;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.google.common.reflect.TypeToken;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.SubscriberBuilder;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class LightbendReactiveMessagingCdiExtension implements Extension {

  private final Collection<StreamsInjectionTarget<?>> allStreamingBeans = new ArrayList<>();

  public void registerBeans(@Observes BeforeBeanDiscovery bbd) {
    bbd.addAnnotatedType(StreamManagerImpl.class, StreamManagerImpl.class.getName());
  }

  public <T> void locateStreams(@Observes ProcessInjectionTarget<T> bean, BeanManager beanManager) {

    // Find all the stream annotated methods, and read them.
    List<StreamDescriptor> streams = locateStreams(bean.getAnnotatedType());

    if (!streams.isEmpty()) {
      StreamsInjectionTarget<T> newTarget = new StreamsInjectionTarget<>(beanManager, bean.getAnnotatedType(), bean.getInjectionTarget(), streams);
      allStreamingBeans.add(newTarget);
      bean.setInjectionTarget(newTarget);
    }
  }

  public void validateStreams(@Observes AfterDeploymentValidation adv) {
    for (StreamsInjectionTarget<?> streamingBean : allStreamingBeans) {
      streamingBean.validate();
    }
  }

  public void startApplicationScopedStreams(@Observes @Initialized(ApplicationScoped.class) Object obj, BeanManager beanManager) {
    for (StreamsInjectionTarget<?> streamingBean : allStreamingBeans) {

      AnnotatedType<?> type = streamingBean.getBeanType();
      // todo do we need to worry about qualifiers?
      for (Bean<?> bean: beanManager.getBeans(type.getBaseType())) {
        if (bean.getScope().equals(ApplicationScoped.class)) {
          beanManager.getReference(bean, bean.getBeanClass(), beanManager.createCreationalContext(bean)).toString();
        }
      }
    }
  }

  private <T> List<StreamDescriptor> locateStreams(AnnotatedType<T> type) {
    List<StreamDescriptor> streams = new ArrayList<>();

    for (AnnotatedMethod<? super T> method : type.getMethods()) {
      if (method.getAnnotation(Incoming.class) != null || method.getAnnotation(Outgoing.class) != null) {
        streams.add(readStreamDescriptor(type, method));
      }
    }

    return streams;
  }

  private StreamDescriptor readStreamDescriptor(AnnotatedType<?> bean, AnnotatedMethod<?> method) {
    TypeToken returnType = TypeToken.of(method.getBaseType());

    // This could probably be extracted into an abstraction that is stored in a list, then we can just iterate
    // through the list instead of a bit if else if chain.
    if (returnType.isSubtypeOf(Processor.class)) {
      return readProcessorShape(bean, method, returnType, Processor.class, StreamShape.RS_PROCESSOR);
    }
    else if (returnType.isSubtypeOf(ProcessorBuilder.class)) {
      return readProcessorShape(bean, method, returnType, ProcessorBuilder.class, StreamShape.PROCESSOR_BUILDER);
    }
    else if (returnType.isSubtypeOf(Flow.class)) {
      return readProcessorShape(bean, method, returnType, Flow.class, StreamShape.AKKA_FLOW);
    }
    else if (returnType.isSubtypeOf(Publisher.class)) {
      return readPublisherShape(bean, method, returnType, Publisher.class, StreamShape.RS_PUBLISHER);
    }
    else if (returnType.isSubtypeOf(PublisherBuilder.class)) {
      return readPublisherShape(bean, method, returnType, PublisherBuilder.class, StreamShape.PUBLISHER_BUILDER);
    }
    else if (returnType.isSubtypeOf(Source.class)) {
      return readPublisherShape(bean, method, returnType, Source.class, StreamShape.AKKA_SOURCE);
    }
    else if (returnType.isSubtypeOf(Subscriber.class)) {
      return readSubscriberShape(bean, method, returnType, Subscriber.class, StreamShape.RS_SUBSCRIBER);
    }
    else if (returnType.isSubtypeOf(SubscriberBuilder.class)) {
      return readSubscriberShape(bean, method, returnType, SubscriberBuilder.class, StreamShape.SUBSCRIBER_BUILDER);
    }
    else if (returnType.isSubtypeOf(Sink.class)) {
      return readSubscriberShape(bean, method, returnType, Sink.class, StreamShape.AKKA_SINK);
    }
    else if (returnType.isSubtypeOf(CompletionStage.class)) {

      if (method.getJavaMember().getParameterCount() != 1) {
        throw new DefinitionException(CompletionStage.class + " returning method " + toString(method) + " must take exactly one parameter for input messages");
      }
      Type inType = method.getJavaMember().getGenericParameterTypes()[0];
      Type outType = Reflections.getTypeArgumentsFor(returnType, CompletionStage.class, "return type of", method)[0];
      return readProcessorShape(bean, method, inType, outType, StreamShape.ASYNCHRONOUS_METHOD);

    }
    else {

      if (method.getJavaMember().getParameterCount() != 1) {
        throw new DefinitionException(CompletionStage.class + " returning method " + toString(method) + " must take exactly one parameter for input messages");
      }
      Type inType = method.getJavaMember().getGenericParameterTypes()[0];
      Type outType = returnType.getRawType();
      return readProcessorShape(bean, method, inType, outType, StreamShape.SYNCHRONOUS_METHOD);
    }
  }

  private StreamDescriptor readProcessorShape(AnnotatedType<?> bean, AnnotatedMethod<?> method, TypeToken returnType, Class<?> processorClass, StreamShape shape) {
    if (method.getJavaMember().getParameterCount() > 0) {
      throw new DefinitionException(processorClass + " returning method " + toString(method) + " must not take parameters.");
    }
    Type[] processorTypes = Reflections.getTypeArgumentsFor(returnType, processorClass, "return type of", method);
    Type inType = processorTypes[0];
    Type outType = processorTypes[1];
    return readProcessorShape(bean, method, inType, outType, shape);
  }

  private StreamDescriptor readProcessorShape(AnnotatedType<?> bean, AnnotatedMethod<?> method, Type inType, Type outType, StreamShape shape) {
    Incoming incoming = method.getAnnotation(Incoming.class);
    if (incoming == null) {
      throw new DefinitionException(Outgoing.class + " annotated method " + toString(method) + " has an input type but no " + Incoming.class + " annotation.");
    }

    Outgoing outgoing = method.getAnnotation(Outgoing.class);

    Pair<Type, Optional<Type>> inMessageType = unwrapMessageType(method, inType);
    Pair<Type, Optional<Type>> outMessageType = unwrapMessageType(method, outType);

    StreamDescriptorPort<Incoming> incomingPort = new StreamDescriptorPort<>(incoming, inMessageType.first(), inMessageType.second());
    Optional<StreamDescriptorPort<Outgoing>> outgoingPort;
    boolean incomingDestinationWrapped = false;
    if (outgoing == null) {
      outgoingPort = Optional.empty();
      incomingDestinationWrapped = outMessageType.second().isPresent();
    }
    else {
      outgoingPort = Optional.of(new StreamDescriptorPort<>(outgoing, outMessageType.first(), outMessageType.second()));
    }

    return new StreamDescriptor(bean, method, Optional.of(incomingPort), outgoingPort, shape, incomingDestinationWrapped);
  }

  private StreamDescriptor readPublisherShape(AnnotatedType<?> bean, AnnotatedMethod<?> method, TypeToken returnType, Class<?> publisherClass, StreamShape shape) {
    if (method.getJavaMember().getParameterCount() > 0) {
      throw new DefinitionException(publisherClass + " returning method " + toString(method) + " must not take parameters.");
    }
    Type messageType = Reflections.getTypeArgumentsFor(returnType, publisherClass, "return type of", method)[0];

    if (method.getAnnotation(Incoming.class) != null) {
      throw new DefinitionException(Incoming.class + " annotated method " + toString(method) + " has no input.");
    }

    Outgoing outgoing = method.getAnnotation(Outgoing.class);
    Pair<Type, Optional<Type>> unwrapped = unwrapMessageType(method, messageType);
    StreamDescriptorPort<Outgoing> outgoingPort = new StreamDescriptorPort<>(outgoing, unwrapped.first(), unwrapped.second());
    return new StreamDescriptor(bean, method, Optional.empty(), Optional.of(outgoingPort), shape, false);
  }

  private StreamDescriptor readSubscriberShape(AnnotatedType<?> bean, AnnotatedMethod<?> method, TypeToken returnType, Class<?> subscriberClass, StreamShape shape) {
    if (method.getJavaMember().getParameterCount() > 0) {
      throw new DefinitionException(subscriberClass + " returning method " + toString(method) + " must not take parameters.");
    }
    Type messageType = Reflections.getTypeArgumentsFor(returnType, subscriberClass, "return type of", method)[0];

    if (method.getAnnotation(Outgoing.class) != null) {
      throw new DefinitionException(Outgoing.class + " annotated method " + toString(method) + " has no output.");
    }

    Incoming incoming = method.getAnnotation(Incoming.class);
    Pair<Type, Optional<Type>> unwrapped = unwrapMessageType(method, messageType);
    StreamDescriptorPort<Incoming> incomingPort = new StreamDescriptorPort<>(incoming, unwrapped.first(), unwrapped.second());
    return new StreamDescriptor(bean, method, Optional.of(incomingPort), Optional.empty(), shape, false);
  }

  private Pair<Type, Optional<Type>> unwrapMessageType(AnnotatedMethod<?> method, Type rawType) {
    TypeToken typeToken = TypeToken.of(rawType);
    if (typeToken.isSubtypeOf(Message.class)) {
      Type messageType = Reflections.getTypeArgumentsFor(typeToken, Message.class, "message type from return type of", method)[0];
      if (messageType instanceof Class || messageType instanceof ParameterizedType) {
        // todo validate parameterized type recursively to ensure it doesn't contain
        // abstract types
        return Pair.create(messageType, Optional.of(typeToken.getType()));
      }
      else {
        throw new DefinitionException("Could not determine message type for " + toString(method));
      }
    }
    else {
      return Pair.create(rawType, Optional.empty());
    }
  }

  private String toString(AnnotatedMethod<?> method) {
    return method.getDeclaringType().getJavaClass().getName() + "." + method.getJavaMember().getName();
  }

}