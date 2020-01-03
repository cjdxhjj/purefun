/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.typeclasses.Functor;

import static java.util.Objects.requireNonNull;

@HigherKind
public final class Fix<F extends Kind> {

  private final Eval<Higher1<F, Fix<F>>> unfix;

  private Fix(Eval<Higher1<F, Fix<F>>> unfix) {
    this.unfix = requireNonNull(unfix);
  }

  public static <F extends Kind> Fix<F> of(Eval<Higher1<F, Fix<F>>> unfix) {
    return new Fix<>(unfix);
  }

  public static <F extends Kind, A> Eval<A> fold(Functor<F> functor, Fix<F> fix, Function1<Higher1<F, A>, Eval<A>> coalg) {
    Eval<Higher1<F, A>> eval = fix.unfix.map(x -> functor.map(x, y -> fold(functor, y, coalg).value()));
    return eval.flatMap(coalg);
  }

  public static <F extends Kind, A> Fix<F> unfold(Functor<F> functor, A initial, Function1<A, Higher1<F, A>> alg) {
    return of(Eval.later(() -> functor.map(alg.apply(initial), a -> unfold(functor, a, alg))));
  }
}
