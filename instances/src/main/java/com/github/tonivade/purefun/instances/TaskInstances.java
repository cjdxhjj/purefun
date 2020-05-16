/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.effect.Task_;

import java.time.Duration;

public interface TaskInstances {

  static Functor<Task_> functor() {
    return TaskFunctor.instance();
  }

  static Applicative<Task_> applicative() {
    return TaskApplicative.instance();
  }

  static Monad<Task_> monad() {
    return TaskMonad.instance();
  }

  static MonadError<Task_, Throwable> monadError() {
    return TaskMonadError.instance();
  }

  static MonadThrow<Task_> monadThrow() {
    return TaskMonadThrow.instance();
  }

  static MonadDefer<Task_> monadDefer() {
    return TaskMonadDefer.instance();
  }

  static <A> Reference<Task_, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }
}

@Instance
interface TaskFunctor extends Functor<Task_> {

  @Override
  default <A, B> Higher1<Task_, B>
          map(Higher1<Task_, A> value, Function1<A, B> map) {
    return Task_.narrowK(value).map(map).kind1();
  }
}

interface TaskPure extends Applicative<Task_> {

  @Override
  default <A> Higher1<Task_, A> pure(A value) {
    return Task.pure(value).kind1();
  }
}

@Instance
interface TaskApplicative extends TaskPure {

  @Override
  default <A, B> Higher1<Task_, B>
          ap(Higher1<Task_, A> value,
             Higher1<Task_, Function1<A, B>> apply) {
    return Task_.narrowK(apply).flatMap(map -> Task_.narrowK(value).map(map)).kind1();
  }
}

@Instance
interface TaskMonad extends TaskPure, Monad<Task_> {

  @Override
  default <A, B> Higher1<Task_, B>
          flatMap(Higher1<Task_, A> value,
                  Function1<A, ? extends Higher1<Task_, B>> map) {
    return Task_.narrowK(value).flatMap(map.andThen(Task_::narrowK)).kind1();
  }
}

@Instance
interface TaskMonadError extends TaskMonad, MonadError<Task_, Throwable> {

  @Override
  default <A> Higher1<Task_, A> raiseError(Throwable error) {
    return Task.<A>raiseError(error).kind1();
  }

  @Override
  default <A> Higher1<Task_, A>
          handleErrorWith(Higher1<Task_, A> value,
                          Function1<Throwable, ? extends Higher1<Task_, A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<Throwable, Task<A>> mapError = handler.andThen(Task_::narrowK);
    Function1<A, Task<A>> map = Task::pure;
    Task<A> task = Task_.narrowK(value);
    return task.foldM(mapError, map).kind1();
  }
}

@Instance
interface TaskMonadThrow
    extends TaskMonadError,
            MonadThrow<Task_> { }

interface TaskDefer extends Defer<Task_> {

  @Override
  default <A> Higher1<Task_, A>
          defer(Producer<Higher1<Task_, A>> defer) {
    return Task.defer(() -> defer.map(Task_::narrowK).get()).kind1();
  }
}

interface TaskBracket extends Bracket<Task_> {

  @Override
  default <A, B> Higher1<Task_, B>
          bracket(Higher1<Task_, A> acquire,
                  Function1<A, ? extends Higher1<Task_, B>> use,
                  Consumer1<A> release) {
    return Task.bracket(acquire.fix1(Task_::narrowK), use.andThen(Task_::narrowK), release).kind1();
  }
}

@Instance
interface TaskMonadDefer
    extends MonadDefer<Task_>, TaskMonadThrow, TaskDefer, TaskBracket {
  @Override
  default Higher1<Task_, Unit> sleep(Duration duration) {
    return Task.sleep(duration).kind1();
  }
}
