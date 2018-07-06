/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.jsonb;

import com.lightbend.microprofile.reactive.messaging.spi.MessageSerializer;

import javax.json.bind.Jsonb;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;

public class JsonbMessageSerializer<T> implements MessageSerializer<T> {
  private final Type type;
  private final Jsonb jsonb;

  public JsonbMessageSerializer(Type type, Jsonb jsonb) {
    this.type = type;
    this.jsonb = jsonb;
  }

  @Override
  public byte[] toBytes(T message) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    jsonb.toJson(message, type, baos);
    return baos.toByteArray();
  }
}
