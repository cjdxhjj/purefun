/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Precondition.checkNonNull;
import java.time.Duration;
import java.util.concurrent.Executor;
import com.github.tonivade.purefun.CheckedRunnable;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@HigherKind
public final class URIO<R, T> implements URIOOf<R, T>, Recoverable {

  private static final URIO<?, Unit> UNIT = new URIO<>(ZIO.unit());

  private final ZIO<R, Nothing, T> instance;

  URIO(ZIO<R, Nothing, T> value) {
    this.instance = checkNonNull(value);
  }

  public T unsafeRunSync(R env) {
    return instance.provide(env).get();
  }

  public Try<T> safeRunSync(R env) {
    return Try.of(() -> unsafeRunSync(env));
  }

  @SuppressWarnings("unchecked")
  public <E> ZIO<R, E, T> toZIO() {
    return (ZIO<R, E, T>) instance;
  }

  @SuppressWarnings("unchecked")
  public <E> EIO<E, T> toEIO() {
    return new EIO<>((ZIO<Nothing, E, T>) instance);
  }

  public Future<T> toFuture(R env) {
    return toFuture(Future.DEFAULT_EXECUTOR, env);
  }

  public Future<T> toFuture(Executor executor, R env) {
    return instance.toFuture(executor, env).map(Either::get);
  }

  public void provideAsync(Executor executor, R env, Consumer1<Try<T>> callback) {
    instance.provideAsync(executor, env, result -> callback.accept(result.map(Either::get)));
  }

  public void provideAsync(R env, Consumer1<Try<T>> callback) {
    provideAsync(Future.DEFAULT_EXECUTOR, env, callback);
  }

  public <F extends Witness> Kind<F, T> foldMap(R env, MonadDefer<F> monad) {
    return instance.foldMap(env, monad);
  }

  public <B> URIO<R, B> map(Function1<T, B> map) {
    return new URIO<>(instance.map(map));
  }

  public <B> URIO<R, B> flatMap(Function1<T, URIO<R, B>> map) {
    return new URIO<>(instance.flatMap(x -> map.apply(x).instance));
  }

  public <B> URIO<R, B> andThen(URIO<R, B> next) {
    return new URIO<>(instance.andThen(next.instance));
  }

  public URIO<R, T> recover(Function1<Throwable, T> mapError) {
    return redeem(mapError, identity());
  }

  @SuppressWarnings("unchecked")
  public <X extends Throwable> URIO<R, T> recoverWith(Class<X> type, Function1<X, T> function) {
    return recover(cause -> {
      if (type.isAssignableFrom(cause.getClass())) {
        return function.apply((X) cause);
      }
      return sneakyThrow(cause);
    });
  }

  public <B> URIO<R, B> redeem(Function1<Throwable, B> mapError, Function1<T, B> map) {
    return redeemWith(mapError.andThen(URIO::pure), map.andThen(URIO::pure));
  }

  public <B> URIO<R, B> redeemWith(Function1<Throwable, URIO<R, B>> mapError, Function1<T, URIO<R, B>> map) {
    return new URIO<>(ZIO.redeem(instance).foldM(error -> mapError.apply(error).instance, x -> map.apply(x).instance));
  }

  public URIO<R, T> repeat() {
    return repeat(1);
  }

  public URIO<R, T> repeat(int times) {
    return repeat(unit(), times);
  }

  public URIO<R, T> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  public URIO<R, T> repeat(Duration delay, int times) {
    return repeat(sleep(delay), times);
  }

  public URIO<R, T> retry() {
    return retry(1);
  }

  public URIO<R, T> retry(int maxRetries) {
    return retry(unit(), maxRetries);
  }

  public URIO<R, T> retry(Duration delay) {
    return retry(delay, 1);
  }

  public URIO<R, T> retry(Duration delay, int maxRetries) {
    return retry(sleep(delay), maxRetries);
  }

  private URIO<R, T> repeat(URIO<R, Unit> pause, int times) {
    return redeemWith(URIO::<R, T>raiseError, value -> {
      if (times > 0) {
        return pause.andThen(repeat(pause, times - 1));
      } else {
        return pure(value);
      }
    });
  }

  private URIO<R, T> retry(URIO<R, Unit> pause, int maxRetries) {
    return redeemWith(error -> {
      if (maxRetries > 0) {
        return pause.andThen(retry(pause.repeat(), maxRetries - 1));
      } else {
        return raiseError(error);
      }
    }, URIO::<R, T>pure);
  }

  static <R, A> URIO<R, A> accessM(Function1<R, URIO<R, A>> map) {
    return new URIO<>(ZIO.accessM(map.andThen(URIO::toZIO)));
  }

  public static <R, A> URIO<R, A> access(Function1<R, A> map) {
    return accessM(map.andThen(URIO::pure));
  }

  public static <R> URIO<R, R> env() {
    return access(identity());
  }

  public static <R, A, B, C> URIO<R, C> map2(URIO<R, A> za, URIO<R, B> zb, Function2<A, B, C> mapper) {
    return new URIO<>(ZIO.map2(za.instance, zb.instance, mapper));
  }

  public static <R> URIO<R, Unit> sleep(Duration delay) {
    return fold(ZIO.sleep(delay));
  }

  public static <R> URIO<R, Unit> exec(CheckedRunnable task) {
    return fold(ZIO.exec(task));
  }

  public static <R, A> URIO<R, A> pure(A value) {
    return new URIO<>(ZIO.pure(value));
  }

  public static <R, A> URIO<R, A> raiseError(Throwable throwable) {
    return new URIO<>(ZIO.fromEither(() -> { throw throwable; }));
  }

  public static <R, A> URIO<R, A> defer(Producer<URIO<R, A>> lazy) {
    return new URIO<>(ZIO.defer(() -> lazy.get().instance));
  }

  public static <R, A> URIO<R, A> task(Producer<A> task) {
    return fold(ZIO.task(task));
  }

  public static <R, A extends AutoCloseable, B> URIO<R, B> bracket(URIO<R, A> acquire, Function1<A, URIO<R, B>> use) {
    return fold(ZIO.bracket(ZIO.redeem(acquire.instance), resource -> ZIO.redeem(use.apply(resource).instance)));
  }

  public static <R, A, B> URIO<R, B> bracket(URIO<R, A> acquire, Function1<A, URIO<R, B>> use, Consumer1<A> release) {
    return fold(ZIO.bracket(ZIO.redeem(acquire.instance), resource -> ZIO.redeem(use.apply(resource).instance), release));
  }

  @SuppressWarnings("unchecked")
  public static <R> URIO<R, Unit> unit() {
    return (URIO<R, Unit>) UNIT;
  }

  private static <R, A> URIO<R, A> fold(ZIO<R, Throwable, A> zio) {
    return new URIO<>(zio.foldM(error -> URIO.<R, A>raiseError(error).instance, value -> URIO.<R, A>pure(value).instance));
  }
}