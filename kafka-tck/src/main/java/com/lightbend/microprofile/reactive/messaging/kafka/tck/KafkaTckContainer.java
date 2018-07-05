package com.lightbend.microprofile.reactive.messaging.kafka.tck;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.eclipse.microprofile.reactive.messaging.tck.spi.TckContainer;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class KafkaTckContainer implements TckContainer {

  private static final Logger log = LoggerFactory.getLogger(KafkaTckContainer.class);

  private AdminClient client() {
    return KafkaAdminClientLoader.INSTANCE;
  }

  @Override
  public boolean supportsIncoming() {
    return true;
  }

  @Override
  public boolean supportsOutgoing() {
    return true;
  }

  @Override
  public boolean mergeArchives() {
    return true;
  }

  @Override
  public List<DeploymentDescription> createDeployments(String... topics) {

    try {
      if (topics.length > 0) {
        Set<String> toDelete = new HashSet<>(Arrays.asList(topics));
        toDelete.retainAll(client().listTopics().names().get());
        if (!toDelete.isEmpty()) {
          log.debug("Deleting existing topics: " + String.join(", ", toDelete));
          client().deleteTopics(toDelete).all().get(testEnvironment().receiveTimeout().toMillis(), TimeUnit.MILLISECONDS);
          log.debug("Waiting for topics to be deleted...");
          Thread.sleep(1000);
        }

        List<NewTopic> newTopics = new ArrayList<>();
        for (String topic : topics) {
          newTopics.add(new NewTopic(topic, 1, (short) 1));
        }

        log.debug("Creating topics: " + String.join(", ", topics));
        client().createTopics(newTopics).all().get();
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }

    return Collections.singletonList(new DeploymentDescription("kafka", ShrinkWrap.create(JavaArchive.class)
        .addClass(KafkaTckLocalContainerController.class)));
  }

  @Override
  public void teardownTopics(String... topics) {
    if (topics.length > 0) {
      try {
        log.debug("Tearing down topics: " + String.join(", ", topics));
        client().deleteTopics(Arrays.asList(topics)).all().get();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
