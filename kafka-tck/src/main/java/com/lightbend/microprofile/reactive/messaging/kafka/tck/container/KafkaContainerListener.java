/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.kafka.tck.container;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;
import org.jboss.arquillian.test.spi.event.suite.BeforeSuite;

public class KafkaContainerListener {

  @ApplicationScoped
  @Inject
  private InstanceProducer<ActorSystem> actorSystem;
  @ApplicationScoped
  @Inject
  private InstanceProducer<Materializer> materializer;

  public void onBeforeSuite(@Observes BeforeSuite beforeSuite) {
    ActorSystem system = ActorSystem.create();
    actorSystem.set(system);
    materializer.set(ActorMaterializer.create(system));
  }

  public void onAfterSuite(@Observes AfterSuite afterSuite) {
    actorSystem.get().terminate();
  }
}
