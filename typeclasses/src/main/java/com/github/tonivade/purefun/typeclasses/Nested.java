/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Kind;

@SuppressWarnings("unused")
public interface Nested<F, G> {

  @SuppressWarnings("unchecked")
  static <F, G, A> Kind<Nested<F, G>, A> nest(Kind<F, ? extends Kind<G, ? extends A>> unnested) {
    return (Kind<Nested<F, G>, A>) unnested;
  }

  @SuppressWarnings("unchecked")
  static <F, G, A> Kind<F, Kind<G, A>> unnest(Kind<Nested<F, G>, ? extends A> nested) {
    return (Kind<F, Kind<G, A>>) nested;
  }
}
