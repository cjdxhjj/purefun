/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.type.Either;

public interface ApplicativeError<F extends Witness, E> extends Applicative<F> {

  <A> Kind<F, A> raiseError(E error);

  <A> Kind<F, A> handleErrorWith(Kind<F, A> value, Function1<? super E, ? extends Kind<F, ? extends A>> handler);

  default <A> Kind<F, A> handleError(Kind<F, A> value, Function1<? super E, ? extends A> handler) {
    return handleErrorWith(value, handler.andThen(this::<A>pure));
  }

  default <A> Kind<F, A> recoverWith(Kind<F, A> value, PartialFunction1<? super E, ? extends Kind<F, ? extends A>> handler) {
    return handleErrorWith(value, error -> handler.andThen(Kind::narrowK).applyOrElse(error, x -> raiseError(error)));
  }

  default <A> Kind<F, A> recover(Kind<F, A> value, PartialFunction1<? super E, ? extends A> handler) {
    return recoverWith(value, handler.andThen(this::<A>pure));
  }

  default <A> Kind<F, Either<E, A>> attempt(Kind<F, A> value) {
    return handleErrorWith(map(value, Either::right), e -> pure(Either.left(e)));
  }

  default <A> Kind<F, A> fromEither(Either<E, ? extends A> value) {
    return value.fold(this::raiseError, this::<A>pure);
  }
}
