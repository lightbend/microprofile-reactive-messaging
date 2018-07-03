package com.lightbend.microprofile.reactive.messaging.kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.jboss.arquillian.container.spi.event.container.AfterStart;
import org.jboss.arquillian.container.spi.event.container.AfterStop;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.LoadableExtension;

import java.util.Properties;

public class KafkaAdminClientLoader implements LoadableExtension {

  static AdminClient INSTANCE;

  @Override
  public void register(ExtensionBuilder builder) {
    builder.observer(KafkaAdminClientListener.class);
  }

  public static class KafkaAdminClientListener {
    public void onAfterStart(@Observes AfterStart afterStart) {
      Properties properties = new Properties();
      properties.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

      INSTANCE = AdminClient.create(properties);
    }

    public void onAfterStop(@Observes AfterStop afterStop) {
      INSTANCE.close();
    }
  }
}
