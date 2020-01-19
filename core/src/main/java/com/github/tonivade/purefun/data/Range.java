/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.data;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Tuple;

import java.util.Iterator;
import java.util.stream.IntStream;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.type.Validation.map2;
import static com.github.tonivade.purefun.type.Validation.requireGreaterThan;
import static com.github.tonivade.purefun.type.Validation.requireGreaterThanOrEqual;
import static com.github.tonivade.purefun.type.Validation.requireLowerThan;

public final class Range implements Iterable<Integer> {

  private final int begin;
  private final int end;

  private Range(int begin, int end) {
    this.begin = begin;
    this.end = end;
  }

  public boolean contains(int value) {
    return map2(
        requireGreaterThanOrEqual(value, begin),
        requireLowerThan(value, end), Tuple::of).isValid();
  }

  public int size() {
    return end - begin;
  }

  public Sequence<Integer> collect() {
    return map(identity());
  }

  public <T> Sequence<T> map(Function1<Integer, T> map) {
    return ImmutableArray.from(stream().boxed()).map(map);
  }

  public IntStream stream() {
    return IntStream.range(begin, end);
  }

  @Override
  public Iterator<Integer> iterator() {
    return stream().iterator();
  }

  public static Range of(int begin, int end) {
    return map2(
        requireLowerThan(begin, end),
        requireGreaterThan(end, begin), Range::new).getOrElseThrow();
  }
}