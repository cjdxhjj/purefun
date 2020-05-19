/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import java.time.Duration;
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.effect.TaskOf;
import com.github.tonivade.purefun.effect.Task_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bracket;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Reference;

public interface TaskInstances {

  static Functor<Task_> functor() {
    return TaskFunctor.INSTANCE;
  }

  static Applicative<Task_> applicative() {
    return TaskApplicative.INSTANCE;
  }

  static Monad<Task_> monad() {
    return TaskMonad.INSTANCE;
  }

  static MonadError<Task_, Throwable> monadError() {
    return TaskMonadError.INSTANCE;
  }

  static MonadThrow<Task_> monadThrow() {
    return TaskMonadThrow.INSTANCE;
  }

  static MonadDefer<Task_> monadDefer() {
    return TaskMonadDefer.INSTANCE;
  }

  static <A> Reference<Task_, A> ref(A value) {
    return Reference.of(monadDefer(), value);
  }
}

interface TaskFunctor extends Functor<Task_> {

  TaskFunctor INSTANCE = new TaskFunctor() {};

  @Override
  default <A, B> Higher1<Task_, B>
          map(Higher1<Task_, A> value, Function1<A, B> map) {
    return TaskOf.narrowK(value).map(map);
  }
}

interface TaskPure extends Applicative<Task_> {

  @Override
  default <A> Higher1<Task_, A> pure(A value) {
    return Task.pure(value);
  }
}

interface TaskApplicative extends TaskPure {

  TaskApplicative INSTANCE = new TaskApplicative() {};

  @Override
  default <A, B> Higher1<Task_, B>
          ap(Higher1<Task_, A> value,
             Higher1<Task_, Function1<A, B>> apply) {
    return TaskOf.narrowK(apply).flatMap(map -> TaskOf.narrowK(value).map(map));
  }
}

interface TaskMonad extends TaskPure, Monad<Task_> {

  TaskMonad INSTANCE = new TaskMonad() {};

  @Override
  default <A, B> Higher1<Task_, B>
          flatMap(Higher1<Task_, A> value,
                  Function1<A, ? extends Higher1<Task_, B>> map) {
    return TaskOf.narrowK(value).flatMap(map.andThen(TaskOf::narrowK));
  }
}

interface TaskMonadError extends TaskMonad, MonadError<Task_, Throwable> {

  TaskMonadError INSTANCE = new TaskMonadError() {};

  @Override
  default <A> Higher1<Task_, A> raiseError(Throwable error) {
    return Task.<A>raiseError(error);
  }

  @Override
  default <A> Higher1<Task_, A>
          handleErrorWith(Higher1<Task_, A> value,
                          Function1<Throwable, ? extends Higher1<Task_, A>> handler) {
    // XXX: java8 fails to infer types, I have to do this in steps
    Function1<Throwable, Task<A>> mapError = handler.andThen(TaskOf::narrowK);
    Function1<A, Task<A>> map = Task::pure;
    Task<A> task = TaskOf.narrowK(value);
    return task.foldM(mapError, map);
  }
}

interface TaskMonadThrow extends TaskMonadError, MonadThrow<Task_> {

  TaskMonadThrow INSTANCE = new TaskMonadThrow() {};
}

interface TaskDefer extends Defer<Task_> {

  @Override
  default <A> Higher1<Task_, A>
          defer(Producer<Higher1<Task_, A>> defer) {
    return Task.defer(() -> defer.map(TaskOf::narrowK).get());
  }
}

interface TaskBracket extends Bracket<Task_> {

  @Override
  default <A, B> Higher1<Task_, B>
          bracket(Higher1<Task_, A> acquire,
                  Function1<A, ? extends Higher1<Task_, B>> use,
                  Consumer1<A> release) {
    return Task.bracket(acquire.fix1(TaskOf::narrowK), use.andThen(TaskOf::narrowK), release);
  }
}

interface TaskMonadDefer
    extends MonadDefer<Task_>, TaskMonadThrow, TaskDefer, TaskBracket {

  TaskMonadDefer INSTANCE = new TaskMonadDefer() {};

  @Override
  default Higher1<Task_, Unit> sleep(Duration duration) {
    return Task.sleep(duration);
  }
}
