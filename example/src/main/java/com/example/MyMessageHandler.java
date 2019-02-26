/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.example;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyMessageHandler {

  @Incoming("my.messages.in1")
  @Outgoing("my.messages.out1")
  public ProcessorBuilder<String, String> processMessages() {
    return ReactiveStreams.<String>builder()
        .map(msg -> {
          System.out.println("Got this message: " + msg);
          return "processed " + msg;
        });
  }
}
