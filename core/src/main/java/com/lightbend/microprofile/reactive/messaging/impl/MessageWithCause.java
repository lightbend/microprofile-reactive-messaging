/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.impl;

import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * This message implementation is used in cases where attributes from a message that caused this message, such as the
 * ack function, are carried with the message. It's primarily useful for debugging purposes, in particular so the
 * toString message can tell us which incoming message this outgoing message is associated with.
 */
class MessageWithCause<T> implements Message<T> {
  private final Message<?> incomingMessage;
  private final Message<T> message;

  MessageWithCause(Message<?> incomingMessage,Message<T> message) {
    this.incomingMessage = incomingMessage;
    this.message = message;
  }

  @Override
  public T getPayload() {
    return message.getPayload();
  }

  @Override
  public CompletionStage<Void> ack() {
    return CompletableFuture.allOf(incomingMessage.ack().toCompletableFuture(),
        message.ack().toCompletableFuture());
  }

  @Override
  public String toString() {
    return "MessageWithCause{" +
        "incomingMessage=" + incomingMessage +
        ", message=" + message +
        '}';
  }
}
