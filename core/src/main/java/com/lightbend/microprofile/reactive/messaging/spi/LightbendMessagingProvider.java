package com.lightbend.microprofile.reactive.messaging.spi;

import org.eclipse.microprofile.reactive.messaging.MessagingProvider;

/**
 * A messaging provider for Lightbend messaging.
 *
 * Implementations of this class must register themselves as a
 * {@link javax.enterprise.context.ApplicationScoped} bean.
 */
public interface LightbendMessagingProvider {

  /**
   * The messaging provider marker that this provider is for.
   */
  Class<? extends MessagingProvider> providerFor();

  /**
   * Validate the given publishing stream.
   */
  <T> ValidatedPublishingStream<T> validatePublishingStream(PublishingStream<T> stream);

  /**
   * Validate the given subscribing stream.
   */
  <T> ValidatedSubscribingStream<T> validateSubscribingStream(SubscribingStream<T> stream);
}
