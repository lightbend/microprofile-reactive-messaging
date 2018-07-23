/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.kafka.tck.client;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.eclipse.microprofile.reactive.messaging.tck.client.event.DeployTopics;
import org.eclipse.microprofile.reactive.messaging.tck.client.event.UnDeployTopics;
import org.eclipse.microprofile.reactive.messaging.tck.container.TestEnvironment;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class KafkaTopicDeployListener {

  private static final Logger log = LoggerFactory.getLogger(KafkaTopicDeployListener.class);

  @Inject
  private Instance<AdminClient> adminClient;
  @Inject
  private Instance<TestEnvironment> testEnvironment;

  public void onDeployTopics(@Observes DeployTopics deployTopics) throws ExecutionException, InterruptedException, TimeoutException {
    List<String> topics = deployTopics.getTopics();
    AdminClient client = adminClient.get();

    if (topics.size() > 0) {
      Set<String> toDelete = new HashSet<>(topics);
      toDelete.retainAll(client.listTopics().names().get());
      if (!toDelete.isEmpty()) {
        log.debug("Deleting existing topics: " + String.join(", ", toDelete));
        client.deleteTopics(toDelete).all().get(testEnvironment.get().receiveTimeout().toMillis(), TimeUnit.MILLISECONDS);
        log.debug("Waiting for topics to be deleted...");
        Thread.sleep(1000);
      }

      List<NewTopic> newTopics = new ArrayList<>();
      for (String topic : topics) {
        newTopics.add(new NewTopic(topic, 1, (short) 1));
      }

      log.debug("Creating topics: " + String.join(", ", topics));
      client.createTopics(newTopics).all().get();
    }
  }

  public void onUnDeployTopics(@Observes UnDeployTopics unDeployTopics) throws ExecutionException, InterruptedException {
    List<String> topics = unDeployTopics.getTopics();

    if (topics.size() > 0) {
      log.debug("Tearing down topics: " + String.join(", ", topics));
      adminClient.get().deleteTopics(topics).all().get();
    }
  }
}
