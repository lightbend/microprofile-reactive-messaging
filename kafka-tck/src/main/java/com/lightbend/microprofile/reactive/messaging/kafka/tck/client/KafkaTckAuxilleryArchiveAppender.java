/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.kafka.tck.client;

import com.lightbend.microprofile.reactive.messaging.kafka.tck.container.KafkaArquillianTckRemoteExtension;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.container.test.spi.client.deployment.AuxiliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

public class KafkaTckAuxilleryArchiveAppender implements AuxiliaryArchiveAppender {

  @Override
  public Archive<?> createAuxiliaryArchive() {
    return ShrinkWrap.create(JavaArchive.class)
        .addPackage(KafkaArquillianTckRemoteExtension.class.getPackage())
        .addAsServiceProvider(RemoteLoadableExtension.class, KafkaArquillianTckRemoteExtension.class);
  }
}
