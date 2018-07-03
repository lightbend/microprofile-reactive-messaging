package com.lightbend.microprofile.reactive.messaging.akka;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class AkkaProvider {
  private final ActorSystem system = ActorSystem.create();
  private final Materializer materializer = ActorMaterializer.create(system);

  @Produces
  public ActorSystem createActorSystem() {
    return system;
  }

  @Produces
  public Materializer provideMaterializer() {
    return materializer;
  }

  @PreDestroy
  public void shutdownActorSystem() {
    system.terminate();
  }
}
