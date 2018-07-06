/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.impl;

/**
 * The shape of the stream returned by the stream method
 */
enum StreamShape {
  PUBLISHER_BUILDER,
  PROCESSOR_BUILDER,
  SUBSCRIBER_BUILDER,
  RS_PUBLISHER,
  RS_PROCESSOR,
  RS_SUBSCRIBER,
  AKKA_SOURCE,
  AKKA_FLOW,
  AKKA_SINK,
  ASYNCHRONOUS_METHOD,
  SYNCHRONOUS_METHOD
}
