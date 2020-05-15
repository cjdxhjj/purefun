/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import java.util.Objects;

@FunctionalInterface
public interface Precondition {

  boolean apply();

  default Precondition negate() {
    return () -> !apply();
  }

  default Precondition and(Precondition other) {
    return () -> apply() && other.apply();
  }

  static Precondition nonNull(Object value) {
    return () -> Objects.nonNull(value);
  }

  static Precondition empty(String value) {
    return value::isEmpty;
  }

  static Precondition nonEmpty(String value) {
    return empty(value).negate();
  }

  static Precondition positive(int value) {
    return greaterThan(value, 0);
  }

  static Precondition negative(int value) {
    return lowerThan(value, 0);
  }

  static Precondition greaterThan(int value, int min) {
    return () -> value > min;
  }

  static Precondition greaterThanOrEquals(int value, int min) {
    return () -> value >= min;
  }

  static Precondition lowerThan(int value, int max) {
    return () -> value < max;
  }

  static Precondition lowerThanOrEquals(int value, int max) {
    return () -> value <= max;
  }

  static Precondition range(int value, int min, int max) {
    return greaterThan(max, min)
        .and(greaterThanOrEquals(value, min))
        .and(lowerThan(value, max));
  }

  static <T> T checkNonNull(T value) {
    check(nonNull(value));
    return value;
  }

  static String checkNonEmpty(String value) {
    check(nonNull(value).and(nonEmpty(value)));
    return value;
  }

  static int checkPositive(int value) {
    check(positive(value));
    return value;
  }

  static int checkNegative(int value) {
    check(negative(value));
    return value;
  }

  static int checkRange(int value, int min, int max) {
    check(range(value, min, max));
    return value;
  }

  static void check(Precondition precondition) {
    require(precondition, IllegalArgumentException::new);
  }

  static void check(Precondition precondition, Producer<String> message) {
    require(precondition, message.andThen(IllegalArgumentException::new));
  }

  static <X extends RuntimeException> void require(Precondition precondition, Producer<X> exception) {
    if (!precondition.apply()) {
      throw exception.get();
    }
  }
}
