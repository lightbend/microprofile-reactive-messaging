package com.example;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class ActorSystemProducer {

  private final ActorSystem system = ActorSystem.create();
  private final Materializer materializer = ActorMaterializer.create(system);

  @Produces
  public ActorSystem getSystem() {
    return system;
  }

  @Produces
  public Materializer getMaterializer() {
    return materializer;
  }

  @PreDestroy
  public void shutdown() {
    system.terminate();
  }
}
