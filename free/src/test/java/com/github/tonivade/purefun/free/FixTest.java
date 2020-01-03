/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.typeclasses.Functor;
import org.junit.jupiter.api.Test;

import static com.github.tonivade.purefun.type.Eval.later;

public class FixTest {

  @Test
  public void test() {
    ListF.cons("1", ListF.cons("2", ListF.nil()));
  }

}

@HigherKind
interface ListF<T> {

  static <T> Fix<ListF.µ> nil() {
    return Fix.of(later(() -> new Nil<Fix<ListF.µ>>().kind1()));
  }

  static Fix<ListF.µ> cons(String head, Fix<ListF.µ> tail) {
    return Fix.of(later(() -> new Cons<>(head, tail).kind1()));
  }

  final class Nil<T> implements ListF<T> { }

  final class Cons<T> implements ListF<T> {

    private final String head;
    private final T tail;

    public Cons(String head, T tail) {
      this.head = head;
      this.tail = tail;
    }

    public String head() {
      return head;
    }

    public T tail() {
      return tail;
    }
  }
}

final class ListFunctor implements Functor<ListF.µ> {
  @Override
  public <T, R> Higher1<ListF.µ, R> map(Higher1<ListF.µ, T> value, Function1<T, R> map) {
    ListF<T> listF = value.fix1(ListF::narrowK);
    if (listF instanceof ListF.Nil) {
      return new ListF.Nil<R>().kind1();
    }
    if (listF instanceof ListF.Cons) {
      ListF.Cons<T> cons = (ListF.Cons<T>) listF;
      return new ListF.Cons<>(cons.head(), map.apply(cons.tail())).kind1();
    }
    return null;
  }
}
