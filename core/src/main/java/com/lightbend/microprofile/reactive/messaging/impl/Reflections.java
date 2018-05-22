package com.lightbend.microprofile.reactive.messaging.impl;

import com.google.common.reflect.TypeToken;

import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.DefinitionException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

class Reflections {
  private Reflections() {}

  static Type[] getTypeArgumentsFor(TypeToken type, Class<?> classFor, String description, AnnotatedMethod<?> method) {
    if (type.getRawType().equals(classFor)) {
      return getTypeArguments(type, description, method);
    } else {
      // Not sure why this cast is needed, TypeSet implements Set<TypeToken<?>>
      for (TypeToken<?> supertype: (Set<TypeToken<?>>) type.getTypes()) {
        if (supertype.getRawType().equals(classFor)) {
          return getTypeArguments(supertype, description, method);
        }
      }
    }
    throw new DefinitionException("Cannot get the type arguments for " + type.getType() + " " + description + " " + method.getDeclaringType().getJavaClass().getName() + "." + method.getJavaMember().getName() + " due to it not implementing " + classFor);
  }

  static Type[] getTypeArguments(TypeToken type, String description, AnnotatedMethod<?> method) {
    if (type.getType() instanceof ParameterizedType) {
      return ((ParameterizedType) type.getType()).getActualTypeArguments();
    } else {
      throw new DefinitionException("Cannot find the type arguments for " + type + " " + description + " " + method.getDeclaringType().getJavaClass().getName() + "." + method.getJavaMember().getName() + " since it is not a parameterized type, instead, it was a " + type.getType().getClass());
    }
  }
}