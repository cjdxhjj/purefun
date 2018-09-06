/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Functor<W extends Witness, T> extends Higher<W, T> {

  <R> Functor<W, R> map(Function1<T, R> map);

}