/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.kafka.tck.container;

import org.eclipse.microprofile.reactive.messaging.tck.container.TckMessagingPuppet;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;

public class KafkaArquillianTckRemoteExtension implements RemoteLoadableExtension {

  @Override
  public void register(ExtensionBuilder builder) {
    builder.observer(KafkaContainerListener.class)
        .service(TckMessagingPuppet.class, KafkaTckMessagingPuppet.class);
  }
}
