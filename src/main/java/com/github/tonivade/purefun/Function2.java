/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

@FunctionalInterface
public interface Function2<T, V, R> {

  R apply(T t, V v);

  default Function1<T, Function1<V, R>> curried() {
    return t -> v -> apply(t, v);
  }

  default Function1<Tuple2<T, V>, R> tupled() {
    return tuple -> apply(tuple.get1(), tuple.get2());
  }

  default <U> Function2<T, V, U> andThen(Function1<R, U> after) {
    return (t, v) -> after.apply(apply(t, v));
  }

  default <U> Function1<U, R> compose(Function1<U, T> beforeT, Function1<U, V> beforeV) {
    return value -> apply(beforeT.apply(value), beforeV.apply(value));
  }

  default Function2<T, V, Try<R>> liftTry() {
    return (t, v) -> Try.of(() -> apply(t, v));
  }

  default Function2<T, V, Either<Throwable, R>> liftEither() {
    return liftTry().andThen(Try::toEither);
  }

  default Function2<T, V, Option<R>> liftOption() {
    return liftTry().andThen(Try::toOption);
  }

  static <T, V, R> Function2<T, V, R> of(Function2<T, V, R> reference) {
    return reference;
  }
}
