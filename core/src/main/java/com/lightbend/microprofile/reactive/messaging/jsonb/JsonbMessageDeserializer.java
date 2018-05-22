package com.lightbend.microprofile.reactive.messaging.jsonb;

import com.lightbend.microprofile.reactive.messaging.spi.MessageDeserializer;

import javax.json.bind.Jsonb;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;

public class JsonbMessageDeserializer<T> implements MessageDeserializer<T> {
  private final Type type;
  private final Jsonb jsonb;

  public JsonbMessageDeserializer(Type type, Jsonb jsonb) {
    this.type = type;
    this.jsonb = jsonb;
  }

  @Override
  public T fromBytes(byte[] bytes) {
    return jsonb.fromJson(new ByteArrayInputStream(bytes), type);
  }
}
