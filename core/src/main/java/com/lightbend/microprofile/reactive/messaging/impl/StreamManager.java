package com.lightbend.microprofile.reactive.messaging.impl;

public interface StreamManager {
  StreamRunner validateStream(StreamDescriptor descriptor);
}
