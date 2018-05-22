package com.lightbend.microprofile.reactive.messaging.kafka;

import com.lightbend.microprofile.reactive.messaging.spi.MessageDeserializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface KafkaConsumerSettings {
  String groupId() default "";
  String bootstrapServers() default "";
  Class<? extends MessageDeserializer> keyDeserializer() default MessageDeserializer.class;
}
