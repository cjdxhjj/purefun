/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UncheckedIOException;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Console;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.Reference;

public interface IOInstances {

  static Functor<IO.µ> functor() {
    return new IOFunctor() {};
  }

  static Monad<IO.µ> monad() {
    return new IOMonad() {};
  }

  static Comonad<IO.µ> comonad() {
    return new IOComonad() {};
  }

  static Defer<IO.µ> defer() {
    return new IODefer() {};
  }

  static MonadError<IO.µ, Throwable> monadError() {
    return new IOMonadError() {};
  }

  static MonadDefer<IO.µ> monadDefer() {
    return new IOMonadDefer() {};
  }

  static <A> Reference<IO.µ, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }

  static Console<IO.µ> console() {
    return new ConsoleIO();
  }
}

@Instance
interface IOFunctor extends Functor<IO.µ> {

  @Override
  default <T, R> IO<R> map(Higher1<IO.µ, T> value, Function1<T, R> map) {
    return IO.narrowK(value).map(map);
  }
}

@Instance
interface IOComonad extends IOFunctor, Comonad<IO.µ> {

  @Override
  default <A, B> Higher1<IO.µ, B> coflatMap(Higher1<IO.µ, A> value, Function1<Higher1<IO.µ, A>, B> map) {
    return IO.task(() -> map.apply(value));
  }

  @Override
  default <A> A extract(Higher1<IO.µ, A> value) {
    return IO.narrowK(value).unsafeRunSync();
  }
}

@Instance
interface IOMonad extends Monad<IO.µ> {

  @Override
  default <T> IO<T> pure(T value) {
    return IO.pure(value);
  }

  @Override
  default <T, R> IO<R> flatMap(Higher1<IO.µ, T> value, Function1<T, ? extends Higher1<IO.µ, R>> map) {
    return IO.narrowK(value).flatMap(map.andThen(IO::narrowK));
  }
}

@Instance
interface IOMonadError extends MonadError<IO.µ, Throwable>, IOMonad {

  @Override
  default <A> IO<A> raiseError(Throwable error) {
    return IO.raiseError(error);
  }

  @Override
  default <A> IO<A> handleErrorWith(Higher1<IO.µ, A> value, Function1<Throwable, ? extends Higher1<IO.µ, A>> handler) {
    return IO.narrowK(value).redeemWith(handler.andThen(IO::narrowK), this::pure);
  }
}

@Instance
interface IODefer extends Defer<IO.µ> {

  @Override
  default <A> IO<A> defer(Producer<Higher1<IO.µ, A>> defer) {
    return IO.suspend(defer.map(IO::narrowK));
  }
}

@Instance
interface IOMonadDefer extends MonadDefer<IO.µ>, IOMonadError, IODefer {

  @Override
  default <A, B> IO<B> bracket(Higher1<IO.µ, A> acquire, Function1<A, ? extends Higher1<IO.µ, B>> use, Consumer1<A> release) {
    return IO.bracket(IO.narrowK(acquire), use.andThen(IO::narrowK), release::accept);
  }
}

@Instance
final class ConsoleIO implements Console<IO.µ> {

  private final SystemConsole console = new SystemConsole();

  @Override
  public IO<String> readln() {
    return IO.task(console::readln);
  }

  @Override
  public IO<Unit> println(String text) {
    return IO.exec(() -> console.println(text));
  }
}

final class SystemConsole {

  void println(String message) {
    writer().println(message);
  }

  String readln() {
    try {
      return reader().readLine();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private BufferedReader reader() {
    return new BufferedReader(new InputStreamReader(System.in));
  }

  private PrintWriter writer() {
    return new PrintWriter(System.out, true);
  }
}