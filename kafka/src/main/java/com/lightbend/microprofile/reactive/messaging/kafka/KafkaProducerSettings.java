package com.lightbend.microprofile.reactive.messaging.kafka;

import com.lightbend.microprofile.reactive.messaging.spi.MessageSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface KafkaProducerSettings {
  String bootstrapServers() default "";
  Class<? extends MessageSerializer> keySerializer() default MessageSerializer.class;
}
