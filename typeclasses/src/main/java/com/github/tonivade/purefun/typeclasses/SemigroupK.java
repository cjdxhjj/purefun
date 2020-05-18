/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Kind;

public interface SemigroupK<F extends Kind> {

  <T> Higher1<F, T> combineK(Higher1<F, T> t1, Higher1<F, T> t2);
}
