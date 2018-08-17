/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface CheckedFunction1<T, R> extends Recoverable {

  R apply(T value) throws Exception;

  default <V> CheckedFunction1<T, V> andThen(CheckedFunction1<R, V> after) {
    return (T value) -> after.apply(apply(value));
  }

  default <V> CheckedFunction1<V, R> compose(CheckedFunction1<V, T> before) {
    return (V value) -> apply(before.apply(value));
  }

  default Function1<T, R> recover(Function1<Throwable, R> mapper) {
    return value -> {
      try {
        return apply(value);
      } catch(Exception e) {
        return mapper.apply(e);
      }
    };
  }

  default Function1<T, R> unchecked() {
    return recover(this::sneakyThrow);
  }

  static <T> CheckedFunction1<T, T> identity() {
    return value -> value;
  }

  static <T, X extends Exception> CheckedFunction1<T, T> failure(Producer<X> supplier) {
    return value -> { throw supplier.get(); };
  }

  static <T, R> CheckedFunction1<T, R> of(CheckedFunction1<T, R> reference) {
    return reference;
  }
}
