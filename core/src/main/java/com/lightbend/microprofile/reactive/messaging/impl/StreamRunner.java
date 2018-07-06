/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.microprofile.reactive.messaging.impl;

import java.io.Closeable;

interface StreamRunner {
  Closeable run(Object bean);
}
