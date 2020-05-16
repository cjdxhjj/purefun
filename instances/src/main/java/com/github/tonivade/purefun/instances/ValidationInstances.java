/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.instances;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Instance;
import com.github.tonivade.purefun.Pattern2;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Validation;
import com.github.tonivade.purefun.type.Validation_;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Bifunctor;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.MonadThrow;
import com.github.tonivade.purefun.typeclasses.Selective;
import com.github.tonivade.purefun.typeclasses.Semigroup;

public interface ValidationInstances {

  static <E, T> Eq<Higher2<Validation_, E, T>> eq(Eq<E> errorEq, Eq<T> validEq) {
    return (a, b) -> Pattern2.<Validation<E, T>, Validation<E, T>, Boolean>build()
      .when((x, y) -> x.isInvalid() && y.isInvalid())
        .then((x, y) -> errorEq.eqv(x.getError(), y.getError()))
      .when((x, y) -> x.isValid() && y.isValid())
        .then((x, y) -> validEq.eqv(x.get(), y.get()))
      .otherwise()
        .returns(false)
      .apply(Validation_.narrowK(a), Validation_.narrowK(b));
  }

  static <E> Functor<Higher1<Validation_, E>> functor() {
    return ValidationFunctor.instance();
  }

  static Bifunctor<Validation_> bifunctor() {
    return ValidationBifunctor.instance();
  }

  static <E> Applicative<Higher1<Validation_, E>> applicative(Semigroup<E> semigroup) {
    return ValidationApplicative.instance(semigroup);
  }

  static <E> Selective<Higher1<Validation_, E>> selective(Semigroup<E> semigroup) {
    return ValidationSelective.instance(semigroup);
  }

  static <E> Monad<Higher1<Validation_, E>> monad() {
    return ValidationMonad.instance();
  }

  static <E> MonadError<Higher1<Validation_, E>, E> monadError() {
    return ValidationMonadError.instance();
  }

  static MonadThrow<Higher1<Validation_, Throwable>> monadThrow() {
    return ValidationMonadThrow.instance();
  }
}

@Instance
interface ValidationFunctor<E> extends Functor<Higher1<Validation_, E>> {

  @Override
  default <T, R> Higher2<Validation_, E, R> map(Higher1<Higher1<Validation_, E>, T> value, Function1<T, R> map) {
    return Validation_.narrowK(value).map(map).kind2();
  }
}

@Instance
interface ValidationBifunctor extends Bifunctor<Validation_> {

  @Override
  default <A, B, C, D> Higher2<Validation_, C, D> bimap(Higher2<Validation_, A, B> value,
      Function1<A, C> leftMap, Function1<B, D> rightMap) {
    return Validation_.narrowK(value).mapError(leftMap).map(rightMap).kind2();
  }
}

interface ValidationPure<E> extends Applicative<Higher1<Validation_, E>> {

  @Override
  default <T> Higher2<Validation_, E, T> pure(T value) {
    return Validation.<E, T>valid(value).kind2();
  }
}

interface ValidationApplicative<E> extends ValidationPure<E>, Applicative<Higher1<Validation_, E>> {

  static <E> ValidationApplicative<E> instance(Semigroup<E> semigroup) {
    return () -> semigroup;
  }

  Semigroup<E> semigroup();

  @Override
  default <T, R> Higher2<Validation_, E, R> ap(Higher1<Higher1<Validation_, E>, T> value,
                                                Higher1<Higher1<Validation_, E>, Function1<T, R>> apply) {
    Validation<E, T> validation = value.fix1(Validation_::narrowK);
    Validation<E, Function1<T, R>> validationF = apply.fix1(Validation_::narrowK);

    if (validation.isValid() && validationF.isValid()) {
      return Validation.<E, R>valid(validationF.get().apply(validation.get())).kind2();
    } else if (validation.isInvalid() && validationF.isValid()) {
      return Validation.<E, R>invalid(validation.getError()).kind2();
    } else if (validation.isValid() && validationF.isInvalid()) {
      return Validation.<E, R>invalid(validationF.getError()).kind2();
    }

    return Validation.<E, R>invalid(semigroup().combine(validation.getError(), validationF.getError())).kind2();
  }
}

interface ValidationSelective<E> extends ValidationApplicative<E>, Selective<Higher1<Validation_, E>> {

  static <E> ValidationSelective<E> instance(Semigroup<E> semigroup) {
    return () -> semigroup;
  }

  @Override
  default <A, B> Higher2<Validation_, E, B> select(Higher1<Higher1<Validation_, E>, Either<A, B>> value,
                                                    Higher1<Higher1<Validation_, E>, Function1<A, B>> apply) {
    return Validation.select(value.fix1(Validation_::narrowK), apply.fix1(Validation_::narrowK)).kind2();
  }
}

@Instance
interface ValidationMonad<E> extends ValidationPure<E>, Monad<Higher1<Validation_, E>> {

  @Override
  default <T, R> Higher2<Validation_, E, R> flatMap(Higher1<Higher1<Validation_, E>, T> value,
      Function1<T, ? extends Higher1<Higher1<Validation_, E>, R>> map) {
    return Validation_.narrowK(value).flatMap(map.andThen(Validation_::narrowK)).kind2();
  }
}

@Instance
interface ValidationMonadError<E> extends ValidationMonad<E>, MonadError<Higher1<Validation_, E>, E> {

  @Override
  default <A> Higher2<Validation_, E, A> raiseError(E error) {
    return Validation.<E, A>invalid(error).kind2();
  }

  @Override
  default <A> Higher2<Validation_, E, A> handleErrorWith(Higher1<Higher1<Validation_, E>, A> value,
      Function1<E, ? extends Higher1<Higher1<Validation_, E>, A>> handler) {
    return Validation_.narrowK(value).fold(handler.andThen(Validation_::narrowK), Validation::<E, A>valid).kind2();
  }
}

@Instance
interface ValidationMonadThrow
    extends ValidationMonadError<Throwable>,
            MonadThrow<Higher1<Validation_, Throwable>> { }

