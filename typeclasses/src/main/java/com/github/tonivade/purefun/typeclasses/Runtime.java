/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import java.util.concurrent.Executor;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;
import com.github.tonivade.purefun.concurrent.Future;

public interface Runtime<F extends Witness> {

  <T> T run(Kind<F, T> value);
  
  <T> Future<T> parRun(Kind<F, T> value, Executor executor);
  
  default <T> Future<T> parRun(Kind<F, T> value) {
    return parRun(value, Future.DEFAULT_EXECUTOR);
  }
}