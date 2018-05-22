package com.lightbend.microprofile.reactive.messaging.impl;

import java.io.Closeable;

interface StreamRunner {
  Closeable run(Object bean);
}
