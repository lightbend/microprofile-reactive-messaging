package com.lightbend.microprofile.reactive.messaging.jsonb;

import com.lightbend.microprofile.reactive.messaging.spi.MessageDeserializer;
import com.lightbend.microprofile.reactive.messaging.spi.MessageSerializer;
import com.lightbend.microprofile.reactive.messaging.spi.SerializationSupport;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import java.lang.reflect.Type;

public class JsonbSerializationSupport implements SerializationSupport {
    private final Jsonb jsonb = JsonbBuilder.create();

    @Override
    public <T> MessageSerializer<T> serializerFor(Type type) {
        return new JsonbMessageSerializer<>(type, jsonb);
    }

    @Override
    public <T> MessageDeserializer<T> deserializerFor(Type type) {
        return new JsonbMessageDeserializer<>(type, jsonb);
    }
}
