/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import static com.github.tonivade.purefun.Nothing.nothing;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.type.Future;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Eq;
import com.github.tonivade.purefun.typeclasses.Monad;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.Transformer;

public class OptionTTest {

  final Monad<Id.µ> monad = Id.monad();

  @Test
  public void map() {
    OptionT<Id.µ, String> some = OptionT.some(monad, "abc");

    OptionT<Id.µ, String> map = some.map(String::toUpperCase);

    assertEquals(Id.of("ABC"), map.get());
  }

  @Test
  public void flatMap() {
    OptionT<Id.µ, String> some = OptionT.some(monad, "abc");

    OptionT<Id.µ, String> map = some.flatMap(value -> OptionT.some(monad, value.toUpperCase()));

    assertEquals(Id.of("ABC"), map.get());
  }

  @Test
  public void filter() {
    OptionT<Id.µ, String> some = OptionT.some(monad, "abc");

    OptionT<Id.µ, String> filter = some.filter(String::isEmpty);
    OptionT<Id.µ, String> orElse = OptionT.some(monad, "not empty");

    assertEquals(orElse.get(), filter.getOrElse("not empty"));
  }

  @Test
  public void none() {
    OptionT<Id.µ, String> none = OptionT.none(monad);

    assertAll(
        () -> assertEquals(Id.of(true), none.isEmpty()),
        () -> assertEquals(Id.of("empty"), none.getOrElse("empty")));
  }

  @Test
  public void some() {
    OptionT<Id.µ, String> some = OptionT.some(monad, "abc");

    assertAll(
        () -> assertEquals(Id.of(false), some.isEmpty()),
        () -> assertEquals(Id.of("abc"), some.getOrElse("empty")));
  }

  @Test
  public void mapK() {
    OptionT<IO.µ, String> someIo = OptionT.some(IO.monad(), "abc");

    OptionT<Try.µ, String> someTry = someIo.mapK(Try.monad(), new IOToTryTransformer());

    assertEquals(Try.success("abc"), Try.narrowK(someTry.get()));
  }

  @Test
  public void eq() {
    OptionT<Id.µ, String> some1 = OptionT.some(Id.monad(), "abc");
    OptionT<Id.µ, String> some2 = OptionT.some(Id.monad(), "abc");
    OptionT<Id.µ, String> none1 = OptionT.none(Id.monad());
    OptionT<Id.µ, String> none2 = OptionT.none(Id.monad());

    Eq<Higher2<OptionT.µ, Id.µ, String>> instance = OptionT.eq(Id.eq(Eq.object()));

    assertAll(
        () -> assertTrue(instance.eqv(some1, some2)),
        () -> assertTrue(instance.eqv(none1, none2)),
        () -> assertFalse(instance.eqv(some1, none1)),
        () -> assertFalse(instance.eqv(none2, some2)));
  }

  @Test
  public void monadErrorFuture() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Higher1<OptionT.µ, Future.µ>, Throwable> monadError = OptionT.monadError(Future.monadError());

    Higher1<Higher1<OptionT.µ, Future.µ>, String> pure = monadError.pure("is not ok");
    Higher1<Higher1<OptionT.µ, Future.µ>, String> raiseError = monadError.raiseError(error);
    Higher1<Higher1<OptionT.µ, Future.µ>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Higher1<Higher1<OptionT.µ, Future.µ>, String> ensureOk =
        monadError.ensure(pure, () -> error, value -> "is not ok".equals(value));
    Higher1<Higher1<OptionT.µ, Future.µ>, String> ensureError =
        monadError.ensure(pure, () -> error, value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Try.failure(error), Future.narrowK(OptionT.narrowK(raiseError).value()).await()),
        () -> assertEquals(Try.success(Option.some("not an error")), Future.narrowK(OptionT.narrowK(handleError).value()).await()),
        () -> assertEquals(Try.failure(error), Future.narrowK(OptionT.narrowK(ensureError).value()).await()),
        () -> assertEquals(Try.success(Option.some("is not ok")), Future.narrowK(OptionT.narrowK(ensureOk).value()).await()));
  }

  @Test
  public void monadErrorIO() {
    MonadError<Higher1<OptionT.µ, Id.µ>, Nothing> monadError = OptionT.monadError(Id.monad());

    Higher1<Higher1<OptionT.µ, Id.µ>, String> pure = monadError.pure("is not ok");
    Higher1<Higher1<OptionT.µ, Id.µ>, String> raiseError = monadError.raiseError(nothing());
    Higher1<Higher1<OptionT.µ, Id.µ>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Higher1<Higher1<OptionT.µ, Id.µ>, String> ensureOk =
        monadError.ensure(pure, () -> nothing(), value -> "is not ok".equals(value));
    Higher1<Higher1<OptionT.µ, Id.µ>, String> ensureError =
        monadError.ensure(pure, () -> nothing(), value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Id.of(Option.none()), OptionT.narrowK(raiseError).value()),
        () -> assertEquals(Id.of(Option.some("not an error")), OptionT.narrowK(handleError).value()),
        () -> assertEquals(Id.of(Option.none()), OptionT.narrowK(ensureError).value()),
        () -> assertEquals(Id.of(Option.some("is not ok")), OptionT.narrowK(ensureOk).value()));
  }
}

class IOToTryTransformer implements Transformer<IO.µ, Try.µ> {

  @Override
  public <T> Higher1<Try.µ, T> apply(Higher1<IO.µ, T> from) {
    return Try.of(IO.narrowK(from)::unsafeRunSync);
  }
}
