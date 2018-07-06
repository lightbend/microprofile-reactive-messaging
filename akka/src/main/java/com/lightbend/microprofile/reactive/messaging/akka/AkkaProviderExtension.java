/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.akka;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

public class AkkaProviderExtension implements Extension {

  public void registerAkkaProvider(@Observes BeforeBeanDiscovery bbd) {
    bbd.addAnnotatedType(AkkaProvider.class, AkkaProvider.class.getName());
  }
}
