/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Nothing.nothing;
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
public final class UIO<T> implements UIOOf<T>, Recoverable {

  private static final UIO<Unit> UNIT = new UIO<>(ZIO.unit());

  private final ZIO<Nothing, Nothing, T> instance;

  UIO(ZIO<Nothing, Nothing, T> value) {
    this.instance = checkNonNull(value);
  }

  public T unsafeRunSync() {
    return instance.provide(nothing()).get();
  }

  public Try<T> safeRunSync() {
    return Try.of(this::unsafeRunSync);
  }

  @SuppressWarnings("unchecked")
  public <R, E> ZIO<R, E, T> toZIO() {
    return (ZIO<R, E, T>) instance;
  }

  @SuppressWarnings("unchecked")
  public <E> EIO<E, T> toEIO() {
    return new EIO<>((ZIO<Nothing, E, T>) instance);
  }

  @SuppressWarnings("unchecked")
  public <R> RIO<R, T> toRIO() {
    return new RIO<>((ZIO<R, Throwable, T>) ZIO.redeem(instance));
  }

  @SuppressWarnings("unchecked")
  public <R> URIO<R, T> toURIO() {
    return new URIO<>((ZIO<R, Nothing, T>) instance);
  }

  public Task<T> toTask() {
    return new Task<>(ZIO.redeem(instance));
  }

  public Future<T> toFuture() {
    return instance.toFuture(nothing()).map(Either::get);
  }

  public void async(Executor executor, Consumer1<Try<T>> callback) {
    instance.provideAsync(executor, nothing(), result -> callback.accept(result.map(Either::get)));
  }

  public void async(Consumer1<Try<T>> callback) {
    async(Future.DEFAULT_EXECUTOR, callback);
  }

  public <F extends Witness> Kind<F, T> foldMap(MonadDefer<F> monad) {
    return instance.foldMap(nothing(), monad);
  }

  public <B> UIO<B> map(Function1<T, B> map) {
    return new UIO<>(instance.map(map));
  }

  public <B> UIO<B> flatMap(Function1<T, UIO<B>> map) {
    return new UIO<>(instance.flatMap(x -> map.apply(x).instance));
  }

  public <B> UIO<B> andThen(UIO<B> next) {
    return new UIO<>(instance.andThen(next.instance));
  }

  public UIO<T> recover(Function1<Throwable, T> mapError) {
    return redeem(mapError, identity());
  }

  @SuppressWarnings("unchecked")
  public <X extends Throwable> UIO<T> recoverWith(Class<X> type, Function1<X, T> function) {
    return recover(cause -> {
      if (type.isAssignableFrom(cause.getClass())) {
        return function.apply((X) cause);
      }
      return sneakyThrow(cause);
    });
  }

  public <B> UIO<B> redeem(Function1<Throwable, B> mapError, Function1<T, B> map) {
    return redeemWith(mapError.andThen(UIO::pure), map.andThen(UIO::pure));
  }

  public <B> UIO<B> redeemWith(Function1<Throwable, UIO<B>> mapError, Function1<T, UIO<B>> map) {
    return new UIO<>(ZIO.redeem(instance).foldM(error -> mapError.apply(error).instance, x -> map.apply(x).instance));
  }

  public UIO<T> repeat() {
    return repeat(1);
  }

  public UIO<T> repeat(int times) {
    return repeat(unit(), times);
  }

  public UIO<T> repeat(Duration delay) {
    return repeat(delay, 1);
  }

  public UIO<T> repeat(Duration delay, int times) {
    return repeat(sleep(delay), times);
  }

  public UIO<T> retry() {
    return retry(1);
  }

  public UIO<T> retry(int maxRetries) {
    return retry(unit(), maxRetries);
  }

  public UIO<T> retry(Duration delay) {
    return retry(delay, 1);
  }

  public UIO<T> retry(Duration delay, int maxRetries) {
    return retry(sleep(delay), maxRetries);
  }

  private UIO<T> repeat(UIO<Unit> pause, int times) {
    return redeemWith(UIO::raiseError, value -> {
      if (times > 0) {
        return pause.andThen(repeat(pause, times - 1));
      } else {
        return pure(value);
      }
    });
  }

  private UIO<T> retry(UIO<Unit> pause, int maxRetries) {
    return redeemWith(error -> {
      if (maxRetries > 0) {
        return pause.andThen(retry(pause.repeat(), maxRetries - 1));
      } else {
        return raiseError(error);
      }
    }, UIO::pure);
  }

  public static <A, B, C> UIO<C> map2(UIO<A> za, UIO<B> zb, Function2<A, B, C> mapper) {
    return new UIO<>(ZIO.map2(za.instance, zb.instance, mapper));
  }

  public static UIO<Unit> sleep(Duration delay) {
    return fold(ZIO.sleep(delay));
  }

  public static UIO<Unit> exec(CheckedRunnable task) {
    return fold(ZIO.exec(task));
  }

  public static <A> UIO<A> pure(A value) {
    return new UIO<>(ZIO.pure(value));
  }

  public static <A> UIO<A> raiseError(Throwable throwable) {
    return new UIO<>(ZIO.fromEither(() -> { throw throwable; }));
  }

  public static <A> UIO<A> defer(Producer<UIO<A>> lazy) {
    return new UIO<>(ZIO.defer(() -> lazy.get().instance));
  }

  public static <A> UIO<A> task(Producer<A> task) {
    return fold(ZIO.task(task));
  }

  public static <A extends AutoCloseable, B> UIO<B> bracket(UIO<A> acquire, Function1<A, UIO<B>> use) {
    return fold(ZIO.bracket(ZIO.redeem(acquire.instance), resource -> ZIO.redeem(use.apply(resource).instance)));
  }

  public static <A, B> UIO<B> bracket(UIO<A> acquire, Function1<A, UIO<B>> use, Consumer1<A> release) {
    return fold(ZIO.bracket(ZIO.redeem(acquire.instance), resource -> ZIO.redeem(use.apply(resource).instance), release));
  }

  public static UIO<Unit> unit() {
    return UNIT;
  }

  private static <A> UIO<A> fold(ZIO<Nothing, Throwable, A> zio) {
    return new UIO<>(zio.foldM(error -> UIO.<A>raiseError(error).instance, value -> UIO.pure(value).instance));
  }
}
