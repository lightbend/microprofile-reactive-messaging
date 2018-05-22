package com.lightbend.microprofile.reactive.messaging.kafka;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.ProducerSettings;
import akka.stream.Materializer;
import com.google.common.reflect.TypeToken;
import com.lightbend.microprofile.reactive.messaging.spi.LightbendMessagingProvider;
import com.lightbend.microprofile.reactive.messaging.spi.MessageDeserializer;
import com.lightbend.microprofile.reactive.messaging.spi.MessageSerializer;
import com.lightbend.microprofile.reactive.messaging.spi.PublishingStream;
import com.lightbend.microprofile.reactive.messaging.spi.SubscribingStream;
import com.lightbend.microprofile.reactive.messaging.spi.ValidatedPublishingStream;
import com.lightbend.microprofile.reactive.messaging.spi.ValidatedSubscribingStream;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.MessagingProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.DeploymentException;
import javax.inject.Inject;
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class KafkaMessagingProvider implements LightbendMessagingProvider {

  private final ActorSystem system;
  private final Materializer materializer;
  private final Optional<KafkaInstanceProvider> kafkaInstanceProvider;
  private final BeanManager beanManager;

  @Inject
  public KafkaMessagingProvider(ActorSystem system, Materializer materializer, Instance<KafkaInstanceProvider> kafkaInstanceProviders, BeanManager beanManager) {
    this.system = system;
    this.materializer = materializer;
    this.beanManager = beanManager;


    Iterator<KafkaInstanceProvider> iter = kafkaInstanceProviders.iterator();
    if (iter.hasNext()) {
      this.kafkaInstanceProvider = Optional.of(iter.next());
    } else {
      this.kafkaInstanceProvider = Optional.empty();
    }
  }


  @Override
  public Class<? extends MessagingProvider> providerFor() {
    return Kafka.class;
  }

  @Override
  public <T> ValidatedPublishingStream<T> validatePublishingStream(PublishingStream<T> stream) {
    if (stream.outgoing().topic().equals("")) {
      throw new DeploymentException("Outgoing topic not defined on " + stream.annotated());
    }
    return new KafkaValidatedPublishingStream<>(stream.outgoing().topic(), createProducerSettings(stream), kafkaInstanceProvider,
        annotatedName(stream.annotated()));
  }

  private <T> ProducerSettings<?, T> createProducerSettings(PublishingStream<T> stream) {

    KafkaProducerSettings producerSettingsAnnotation = stream.annotated().getAnnotation(KafkaProducerSettings.class);

    Serializer<?> keySerializer;
    if (producerSettingsAnnotation != null && !producerSettingsAnnotation.keySerializer().equals(MessageSerializer.class)) {
      keySerializer = new KafkaMessageSerializer<>(getBean(producerSettingsAnnotation.keySerializer()));
      validateWrapperType(stream.wrapperType(), stream.annotated(), KafkaProducerMessage.class);
    } else {
      keySerializer = createKeySerializer(stream.wrapperType(), stream.annotated());
    }

    Serializer<T> serializer = new KafkaMessageSerializer<>(stream.createSerializer());

    ProducerSettings<?, T> producerSettings = ProducerSettings.create(system, keySerializer, serializer);

    if (producerSettingsAnnotation != null && !producerSettingsAnnotation.bootstrapServers().equals("")) {
      producerSettings = producerSettings.withBootstrapServers(producerSettingsAnnotation.bootstrapServers());
    }

    return producerSettings;
  }

  @Override
  public <T> ValidatedSubscribingStream<T> validateSubscribingStream(SubscribingStream<T> stream) {
    if (stream.incoming().topic().equals("")) {
      throw new DeploymentException("Incoming topic not defined on " + stream.annotated());
    }
    return new KafkaValidatedSubscribingStream<>(materializer, createConsumerSettings(stream), stream.incoming().topic(), kafkaInstanceProvider,
        annotatedName(stream.annotated()));
  }

  private <T> ConsumerSettings<?, T> createConsumerSettings(SubscribingStream<T> stream) {

    KafkaConsumerSettings consumerSettingsAnnotation = stream.annotated().getAnnotation(KafkaConsumerSettings.class);

    Deserializer<?> keyDeserializer;
    if (consumerSettingsAnnotation != null && !consumerSettingsAnnotation.keyDeserializer().equals(MessageDeserializer.class)) {
      keyDeserializer = new KafkaMessageDeserializer<>(getBean(consumerSettingsAnnotation.keyDeserializer()));
      validateWrapperType(stream.wrapperType(), stream.annotated(), KafkaConsumerMessage.class);
    } else {
      keyDeserializer = createKeyDeserializer(stream.wrapperType(), stream.annotated());
    }

    Deserializer<T> deserializer = new KafkaMessageDeserializer<>(stream.createDeserializer());

    String groupId;
    if (consumerSettingsAnnotation != null && !consumerSettingsAnnotation.groupId().equals("")) {
      groupId = consumerSettingsAnnotation.groupId();
    } else {
      groupId = annotatedName(stream.annotated());
    }

    ConsumerSettings<?, T> consumerSettings = ConsumerSettings.create(system,
        keyDeserializer, deserializer)
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        .withGroupId(groupId);

    if (consumerSettingsAnnotation != null && !consumerSettingsAnnotation.bootstrapServers().equals("")) {
      consumerSettings = consumerSettings.withBootstrapServers(consumerSettingsAnnotation.bootstrapServers());
    }

    return consumerSettings;
  }

  private String annotatedName(Annotated annotated) {
    if (annotated instanceof AnnotatedMember) {
      Member member = ((AnnotatedMethod) annotated).getJavaMember();
      return member.getDeclaringClass().getName() + "." + member.getName();
    } else {
      return annotated.toString();
    }
  }

  private Serializer<?> createKeySerializer(Optional<Type> wrapperType, Annotated annotated) {
    if (wrapperType.isPresent()) {
      TypeToken type = TypeToken.of(wrapperType.get());

      if (type.getRawType().equals(Message.class)) {
        return new ByteArraySerializer();
      } else if (type.getRawType().equals(KafkaProducerMessage.class)) {
        // Find out the key type
        if (type.getType() instanceof ParameterizedType) {
          Type keyType = ((ParameterizedType) type.getType()).getActualTypeArguments()[0];
          if (keyType.equals(byte[].class)) {
            return new ByteArraySerializer();
          } else if (keyType.equals(String.class)) {
            return new StringSerializer();
          } else {
            throw new DeploymentException("Don't know how to serialize Kafka key type " + keyType + " for stream " + annotated);
          }
        } else {
          throw new DeploymentException(KafkaProducerMessage.class + " must be parameterized in stream " + annotated);
        }
      } else {
        throw new DeploymentException("Unsupported message wrapper type " + type.getType() + " for stream " + annotated);
      }
    } else {
      return new ByteArraySerializer();
    }
  }

  private Deserializer<?> createKeyDeserializer(Optional<Type> wrapperType, Annotated annotated) {
    if (wrapperType.isPresent()) {
      TypeToken type = TypeToken.of(wrapperType.get());

      if (type.getRawType().equals(Message.class)) {
        return new ByteArrayDeserializer();
      } else if (type.getRawType().equals(KafkaConsumerMessage.class)) {
        // Find out the key type
        if (type.getType() instanceof ParameterizedType) {
          Type keyType = ((ParameterizedType) type.getType()).getActualTypeArguments()[0];
          if (keyType.equals(byte[].class)) {
            return new ByteArrayDeserializer();
          } else if (keyType.equals(String.class)) {
            return new StringDeserializer();
          } else {
            throw new DeploymentException("Don't know how to deserialize Kafka key type " + keyType + " for stream " + annotated);
          }
        } else {
          throw new DeploymentException(KafkaConsumerMessage.class + " must be parameterized in stream " + annotated);
        }
      } else {
        throw new DeploymentException("Unsupported message wrapper type " + type.getType() + " for stream " + annotated);
      }
    } else {
      return new ByteArrayDeserializer();
    }
  }

  private void validateWrapperType(Optional<Type> wrapperType, Annotated annotated, Class<?> allowedWrapperType) {
    if (wrapperType.isPresent()) {
      TypeToken type = TypeToken.of(wrapperType.get());

      if (!type.getRawType().equals(Message.class) && !type.getRawType().equals(allowedWrapperType)) {
        throw new DeploymentException("Unsupported message wrapper type " + type.getType() + " for stream " + annotated);
      }
    }
  }

  private <T> T getBean(Class<T> beanClass) {
    Iterator<Bean<?>> beans = beanManager.getBeans(beanClass).iterator();
    if (beans.hasNext()) {
      Bean<T> bean = (Bean) beans.next();
      return beanManager.getContext(bean.getScope()).get(bean);
    } else {
      throw new DeploymentException("No beans registered for class " + beanClass);
    }
  }

  private static class KafkaMessageDeserializer<T> implements Deserializer<T> {
    private final MessageDeserializer<T> deserializer;

    private KafkaMessageDeserializer(MessageDeserializer<T> deserializer) {
      this.deserializer = deserializer;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public T deserialize(String topic, byte[] data) {
      return deserializer.fromBytes(data);
    }

    @Override
    public void close() {
    }
  }

  private static class KafkaMessageSerializer<T> implements Serializer<T> {
    private final MessageSerializer<T> serializer;

    KafkaMessageSerializer(MessageSerializer<T> serializer) {
      this.serializer = serializer;
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, T data) {
      return serializer.toBytes(data);
    }

    @Override
    public void close() {
    }
  }
}
