/*
 * Copyright (C) 2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.example;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

import java.net.URL;
import java.util.Enumeration;

public class Application {
  public static void main(String... args) throws Exception {

    Enumeration<URL> beans = Application.class.getClassLoader().getResources("META-INF/beans.xml");
    while(beans.hasMoreElements()) {
      System.out.println(beans.nextElement());
    }

    Weld weld = new Weld();
    weld.setClassLoader(Application.class.getClassLoader());
    WeldContainer container = weld.initialize();

    System.out.println("Press any key to shutdown...");

    System.in.read();

    System.out.println("Shutting down...");

    container.close();
  }
}
