/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.kafka.tck.client;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.jboss.arquillian.container.spi.event.container.AfterStart;
import org.jboss.arquillian.container.spi.event.container.AfterStop;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.util.Properties;

public class KafkaAdminClientListener {

  @ApplicationScoped
  @Inject
  private InstanceProducer<AdminClient> adminClient;

  public void onAfterStart(@Observes AfterStart afterStart) {
    Properties properties = new Properties();
    properties.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

    adminClient.set(AdminClient.create(properties));
  }

  public void onAfterStop(@Observes AfterStop afterStop) {
    adminClient.get().close();
  }
}
