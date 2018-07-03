package com.lightbend.microprofile.reactive.messaging.impl;

import akka.stream.Materializer;
import com.lightbend.microprofile.reactive.messaging.jsonb.JsonbSerializationSupport;
import com.lightbend.microprofile.reactive.messaging.spi.*;
import com.lightbend.microprofile.reactive.streams.akka.AkkaEngine;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.MessagingProvider;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@ApplicationScoped
public class StreamManagerImpl implements StreamManager {
  private final Map<Class<? extends MessagingProvider>, LightbendMessagingProvider> providers = new HashMap<>();
  private final Materializer materializer;
  private final AkkaEngine akkaEngine;
  private final SerializationSupport serializationSupport;

  @Inject
  public StreamManagerImpl(Instance<LightbendMessagingProvider> providers, Instance<SerializationSupport> serializationSupport, Materializer materializer) {
    this.materializer = materializer;
    for (LightbendMessagingProvider provider: providers) {
      this.providers.put(provider.providerFor(), provider);
    }
    this.akkaEngine = new AkkaEngine(materializer);
    Iterator<SerializationSupport> iter = serializationSupport.iterator();
    if (iter.hasNext()) {
      this.serializationSupport = iter.next();
    } else {
      this.serializationSupport = new JsonbSerializationSupport();
    }
  }

  private <T extends MessagingProvider> LightbendMessagingProvider providerFor(Class<T> providerClass, Annotated annotated) {
    LightbendMessagingProvider provider = providers.get(providerClass);
    if (provider == null) {
      throw new DeploymentException("No " + LightbendMessagingProvider.class.getName() + " registered for " + providerClass.getName() + " on " + annotated);
    }
    return provider;
  }

  @Override
  public StreamRunner validateStream(StreamDescriptor descriptor) {
    ValidatedPublishingStream<?> validatedPublishingStream = validatePublisher(descriptor);
    ValidatedSubscribingStream<?> validatedSubscribingStream = validateSubscriber(descriptor);

    if (validatedPublishingStream != null && validatedSubscribingStream != null) {
      return new ValidatedProcessingStreamRunner<>(akkaEngine, validatedSubscribingStream, validatedPublishingStream, descriptor);
    } else if (validatedPublishingStream != null) {
      return new ValidatedPublishingStreamRunner<>(materializer, akkaEngine, validatedPublishingStream, descriptor);
    } else if (validatedSubscribingStream != null) {
      return new ValidatedSubscribingStreamRunner<>(akkaEngine, validatedSubscribingStream, descriptor);
    } else {
      throw new DeploymentException("Stream with no incoming or outcoming: " + descriptor);
    }
  }

  private ValidatedSubscribingStream<?> validateSubscriber(StreamDescriptor descriptor) {
    if (descriptor.getIncoming().isPresent()) {
      StreamDescriptorPort<Incoming> incoming = descriptor.getIncoming().get();
      LightbendMessagingProvider provider = getProvider(descriptor, incoming.getAnnotation().provider());

      SubscribingStream<?> subscribingStream = new SubscribingStreamImpl<>(serializationSupport, incoming.getAnnotation(), incoming.getMessageType(),
          descriptor.getAnnotated(), incoming.getWrapperType());

      return provider.validateSubscribingStream(subscribingStream);
    } else {
      return null;
    }
  }

  private ValidatedPublishingStream<?> validatePublisher(StreamDescriptor descriptor) {
    if (descriptor.getOutgoing().isPresent()) {
      StreamDescriptorPort<Outgoing> outgoing = descriptor.getOutgoing().get();
      LightbendMessagingProvider provider = getProvider(descriptor, outgoing.getAnnotation().provider());

      PublishingStream<?> publishingStream = new PublishingStreamImpl<>(serializationSupport, outgoing.getAnnotation(), outgoing.getMessageType(),
          descriptor.getAnnotated(), outgoing.getWrapperType());

      return provider.validatePublishingStream(publishingStream);
    } else {
      return null;
    }
  }

  private LightbendMessagingProvider getProvider(StreamDescriptor descriptor, Class<? extends MessagingProvider> providerClass) {
    LightbendMessagingProvider provider;
    if (providerClass.equals(MessagingProvider.class)) {
      // todo - make the default provider configurable
      if (providers.isEmpty()) {
        throw new DeploymentException("No providers registered");
      } else {
        provider = providers.values().iterator().next();
      }
    } else {
      provider = providerFor(providerClass, descriptor.getAnnotated());
    }
    return provider;
  }
}
