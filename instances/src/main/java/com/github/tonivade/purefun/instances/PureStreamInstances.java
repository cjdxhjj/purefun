/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import static com.github.tonivade.purefun.core.Precondition.checkNonNull;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.effect.EIO;
import com.github.tonivade.purefun.effect.PureIO;
import com.github.tonivade.purefun.effect.RIO;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.URIO;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.stream.PureStream;
import com.github.tonivade.purefun.stream.PureStreamOf;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface PureStreamInstances {

  static PureStream.Of<IO<?>> ofIO() {
    return PureStream.of(IOInstances.monadDefer());
  }

  static <R> PureStream.Of<Kind<Kind<PureIO<?, ?, ?>, R>, Throwable>> ofPureIO() {
    return PureStream.of(PureIOInstances.monadDefer());
  }

  static PureStream.Of<UIO<?>> ofUIO() {
    return PureStream.of(UIOInstances.monadDefer());
  }

  static PureStream.Of<Kind<EIO<?, ?>, Throwable>> ofEIO() {
    return PureStream.of(EIOInstances.monadDefer());
  }

  static PureStream.Of<Task<?>> ofTask() {
    return PureStream.of(TaskInstances.monadDefer());
  }

  static <R> PureStream.Of<Kind<URIO<?, ?>, R>> ofURIO() {
    return PureStream.of(URIOInstances.monadDefer());
  }

  static <R> PureStream.Of<Kind<RIO<?, ?>, R>> ofRIO() {
    return PureStream.of(RIOInstances.monadDefer());
  }

  @SuppressWarnings("unchecked")
  static <F> Functor<Kind<PureStream<?, ?>, F>> functor() {
    return PureStreamFunctor.INSTANCE;
  }

  static <F> Monad<Kind<PureStream<?, ?>, F>> monad(PureStream.Of<F> streamOf) {
    return PureStreamMonad.instance(checkNonNull(streamOf));
  }
}

interface PureStreamFunctor<F> extends Functor<Kind<PureStream<?, ?>, F>> {

  @SuppressWarnings("rawtypes")
  PureStreamFunctor INSTANCE = new PureStreamFunctor() {};

  @Override
  default <T, R> PureStream<F, R> map(Kind<Kind<PureStream<?, ?>, F>, ? extends T> value, Function1<? super T, ? extends R> mapper) {
    return PureStreamOf.toPureStream(value).map(mapper);
  }
}

interface PureStreamPure<F> extends Applicative<Kind<PureStream<?, ?>, F>> {

  PureStream.Of<F> streamOf();

  @Override
  default <T> PureStream<F, T> pure(T value) {
    return streamOf().pure(value);
  }
}

interface PureStreamMonad<F> extends Monad<Kind<PureStream<?, ?>, F>>, PureStreamPure<F> {

  static <F> PureStreamMonad<F> instance(PureStream.Of<F> streamOf) {
    return () -> streamOf;
  }

  @Override
  default <T, R> PureStream<F, R> flatMap(Kind<Kind<PureStream<?, ?>, F>, ? extends T> value,
      Function1<? super T, ? extends Kind<Kind<PureStream<?, ?>, F>, ? extends R>> mapper) {
    return PureStreamOf.toPureStream(value).flatMap(mapper.andThen(PureStreamOf::toPureStream));
  }
}
