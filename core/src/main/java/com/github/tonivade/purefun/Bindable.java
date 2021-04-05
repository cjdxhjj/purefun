/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Bindable<F extends Witness, A> extends Mappable<F, A> {
  
  @Override
  <R> Bindable<F, R> map(Function1<? super A, ? extends R> mapper);

  <R> Bindable<F, R> flatMap(Function1<? super A, ? extends Kind<F, ? extends R>> mapper);
  
  @SuppressWarnings("unchecked")
  static <F extends Witness, A> Bindable<F, A> narrowK(Kind<F, ? extends A> kind) {
    return (Bindable<F, A>) kind;
  }
}
