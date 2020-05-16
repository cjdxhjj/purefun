/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Eq;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.data.Sequence_;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.SequenceInstances;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Either_;

public class EqTest {

  @Test
  public void sequence() {
    Eq<Higher1<Sequence_, Integer>> instance = SequenceInstances.eq(Eq.any());

    assertAll(
        () -> assertTrue(instance.eqv(listOf(1, 2, 3).kind1(), listOf(1, 2, 3).kind1())),
        () -> assertFalse(instance.eqv(listOf(1, 2, 3).kind1(), listOf(3, 2, 1).kind1())),
        () -> assertFalse(instance.eqv(listOf(1, 2).kind1(), listOf(1, 2, 3).kind1())));
  }

  @Test
  public void either() {
    Either<Integer, String> left1 = Either.left(10);
    Either<Integer, String> left2 = Either.left(10);
    Either<Integer, String> right1 = Either.right("hola");
    Either<Integer, String> right2 = Either.right("hola");

    Eq<Higher2<Either_, Integer, String>> instance = EitherInstances.eq(Eq.any(), Eq.any());

    assertAll(
        () -> assertTrue(instance.eqv(left1.kind2(), left2.kind2())),
        () -> assertTrue(instance.eqv(right1.kind2(), right2.kind2())),
        () -> assertFalse(instance.eqv(left1.kind2(), right1.kind2())),
        () -> assertFalse(instance.eqv(right2.kind2(), left2.kind2())));
  }
}
