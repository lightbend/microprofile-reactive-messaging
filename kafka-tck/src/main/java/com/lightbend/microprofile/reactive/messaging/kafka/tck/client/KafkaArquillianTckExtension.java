/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.kafka.tck.client;

import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class KafkaArquillianTckExtension implements LoadableExtension {

  @Override
  public void register(ExtensionBuilder builder) {
    builder.observer(KafkaAdminClientListener.class)
      .observer(KafkaTopicDeployListener.class)
      .service(AuxiliaryArchiveAppender.class, KafkaTckAuxilleryArchiveAppender.class);
  }

}
