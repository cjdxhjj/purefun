/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

@FunctionalInterface
public interface CheckedProducer<T> extends Recoverable {

  T get() throws Exception;

  default <V> CheckedFunction1<V, T> asFunction() {
    return value -> get();
  }

  default <R> CheckedProducer<R> andThen(CheckedFunction1<T, R> after) {
    return () -> after.apply(get());
  }

  default Producer<T> recover(Function1<Throwable, T> mapper) {
    return () -> {
      try {
        return get();
      } catch(Exception e) {
        return mapper.apply(e);
      }
    };
  }

  default Producer<T> unchecked() {
    return recover(this::sneakyThrow);
  }

  static <T> CheckedProducer<T> unit(T value) {
    return () -> value;
  }

  static <T, X extends Exception> CheckedProducer<T> failure(Producer<X> supplier) {
    return () -> { throw supplier.get(); };
  }

  static <T> CheckedProducer<T> of(CheckedProducer<T> reference) {
    return reference;
  }
}
