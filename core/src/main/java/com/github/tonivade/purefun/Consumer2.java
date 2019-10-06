/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Unit.unit;

/**
 * <p>This interface represents a function that receives two parameters but it doesn't generate any result.</p>
 * <p>It's like a {@code Function2<A, B, Unit>}</p>
 * @param <A> the type of first parameter received by the function
 * @param <B> the type of second parameter received by the function
 */
@FunctionalInterface
public interface Consumer2<A, B> extends Recoverable {

  default void accept(A value1, B value2) {
    try {
      run(value1, value2);
    } catch (Throwable t) {
      sneakyThrow(t);
    }
  }

  void run(A value1, B value2) throws Throwable;

  default Consumer2<A, B> andThen(Consumer2<A, B> after) {
    return (value1, value2) -> { accept(value1, value2); after.accept(value1, value2); };
  }

  default Function2<A, B, Unit> asFunction() {
    return (value1, value2) -> { accept(value1, value2); return unit(); };
  }

  static <A, B> Consumer2<A, B> of(Consumer2<A, B> reference) {
    return reference;
  }
}
