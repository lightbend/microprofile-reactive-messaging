package com.lightbend.microprofile.reactive.messaging.impl;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class StreamsInjectionTarget<T> implements InjectionTarget<T> {
  private final BeanManager beanManager;
  private final AnnotatedType<T> beanType;
  private final InjectionTarget<T> wrapped;
  private final List<StreamDescriptor> descriptors;
  private final Map<T, List<Closeable>> runningStreams = new IdentityHashMap<>(new ConcurrentHashMap<>());

  private List<StreamRunner> validatedRunners;
  private List<T> instancesToStart = new ArrayList<>();

  StreamsInjectionTarget(BeanManager beanManager, AnnotatedType<T> beanType, InjectionTarget<T> wrapped, List<StreamDescriptor> descriptors) {
    this.beanManager = beanManager;
    this.beanType = beanType;
    this.wrapped = wrapped;
    this.descriptors = descriptors;
  }

  void validate() {
    if (descriptors.isEmpty()) {
      validatedRunners = Collections.emptyList();
    }
    else {
      StreamManager streamManager = getStreamManager();

      List<StreamRunner> runners = new ArrayList<>();
      for (StreamDescriptor descriptor : descriptors) {
        runners.add(streamManager.validateStream(descriptor));
      }
      validatedRunners = runners;
    }
    if (!instancesToStart.isEmpty()) {
      List<T> instances = instancesToStart;
      instancesToStart = new ArrayList<>();
      for (T instance : instances) {
        startStreams(instance);
      }
    }
  }

  AnnotatedType<T> getBeanType() {
    return beanType;
  }

  private StreamManager getStreamManager() {
    Bean<StreamManager> streamManagerBean = (Bean<StreamManager>) beanManager.getBeans(StreamManager.class).iterator().next();
    return (StreamManager) beanManager.getReference(streamManagerBean, StreamManager.class, beanManager.createCreationalContext(streamManagerBean));
  }

  @Override
  public void inject(T instance, CreationalContext<T> ctx) {
    wrapped.inject(instance, ctx);
  }

  @Override
  public void postConstruct(T instance) {
    wrapped.postConstruct(instance);

    if (validatedRunners == null) {
      instancesToStart.add(instance);
    }
    else {
      startStreams(instance);
    }
  }

  private void startStreams(T instance) {
    List<Closeable> streams = new ArrayList<>();
    try {
      // Start all streams
      validatedRunners.forEach(runner ->
          streams.add(runner.run(instance))
      );
      runningStreams.put(instance, streams);
    }
    catch (RuntimeException e) {
      stopStreams(streams);
      throw e;
    }
  }

  private void stopStreams(List<Closeable> streams) {
    streams.forEach(stream -> {
      try {
        stream.close();
      }
      catch (Exception ignored) {
        // todo log better
        ignored.printStackTrace();
      }
    });
  }

  @Override
  public void preDestroy(T instance) {
    try {
      List<Closeable> streams = runningStreams.remove(instance);
      if (streams != null) {
        stopStreams(streams);
      }
      else {
        // This is odd, preDestroy invoked with out a bean that was constructed?
        // todo log better
        System.out.println("preDestroy invoked on bean that hadn't been previously passed to postConstruct?");
      }
    }
    finally {
      wrapped.preDestroy(instance);
    }
  }

  @Override
  public T produce(CreationalContext<T> ctx) {
    return wrapped.produce(ctx);
  }

  @Override
  public void dispose(T instance) {
    wrapped.dispose(instance);
  }

  @Override
  public Set<InjectionPoint> getInjectionPoints() {
    return wrapped.getInjectionPoints();
  }
}
