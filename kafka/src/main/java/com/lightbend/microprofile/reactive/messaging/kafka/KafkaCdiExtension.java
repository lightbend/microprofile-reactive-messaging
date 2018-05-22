package com.lightbend.microprofile.reactive.messaging.kafka;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

public class KafkaCdiExtension implements Extension {

  public void registerBeans(@Observes BeforeBeanDiscovery bbd) {
    bbd.addAnnotatedType(KafkaMessagingProvider.class, KafkaMessagingProvider.class.getName());
  }

}
