/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.data.Sequence.asStream;
import static com.github.tonivade.purefun.type.Eval.later;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;

import com.github.tonivade.purefun.Filterable;
import com.github.tonivade.purefun.FlatMap2;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Matcher1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Defer;
import com.github.tonivade.purefun.typeclasses.Monad;

public interface Stream<F extends Kind, T> extends FlatMap2<Stream.µ, F, T>, Filterable<T> {

  final class µ implements Kind {}

  Stream<F, T> head();
  Stream<F, T> tail();

  Stream<F, T> concat(Stream<F, T> other);
  Stream<F, T> append(Higher1<F, T> other);
  Stream<F, T> prepend(Higher1<F, T> other);

  Stream<F, T> take(int n);
  Stream<F, T> drop(int n);

  @Override
  Stream<F, T> filter(Matcher1<T> matcher);
  Stream<F, T> takeWhile(Matcher1<T> matcher);
  Stream<F, T> dropWhile(Matcher1<T> matcher);

  <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator);
  <R> Eval<Higher1<F, R>> foldRight(Eval<R> begin, Function2<T, Eval<R>, Eval<R>> combinator);

  @Override
  <R> Stream<F, R> map(Function1<T, R> map);
  @Override
  <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<Stream.µ, F, R>> map);
  <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper);

  Stream<F, T> repeat();
  Stream<F, T> intersperse(Higher1<F, T> value);

  default Higher1<F, Sequence<T>> asSequence() {
    return foldLeft(ImmutableList.empty(), (acc, a) -> acc.append(a));
  }

  default Higher1<F, String> asString() {
    return foldLeft("", (acc, a) -> acc + a);
  }

  default Higher1<F, Nothing> drain() {
    return foldLeft(nothing(), (acc, a) -> acc);
  }

  static <F extends Kind, T> Stream<F, T> empty(Monad<F> monad, Comonad<F> comonad, Defer<F> defer) {
    return new Nil<>(monad, comonad, defer);
  }

  @SafeVarargs
  static <F extends Kind, T> Stream<F, T> of(Monad<F> monad, Comonad<F> comonad, Defer<F> defer, T... values) {
    return from(monad, comonad, defer, Arrays.stream(values));
  }

  static <F extends Kind, T> Stream<F, T> pure(Monad<F> monad, Comonad<F> comonad, Defer<F> defer, T value) {
    return eval(monad, comonad, defer, monad.pure(value));
  }

  static <F extends Kind, T> Stream<F, T> eval(Monad<F> monad, Comonad<F> comonad, Defer<F> defer, Higher1<F, T> value) {
    return new Cons<>(monad, comonad, defer, value, empty(monad, comonad, defer));
  }

  static <F extends Kind, T> Stream<F, T> from(Monad<F> monad, Comonad<F> comonad, Defer<F> defer, Iterable<T> iterable) {
    return from(monad, comonad, defer, asStream(iterable.iterator()));
  }

  static <F extends Kind, T> Stream<F, T> from(Monad<F> monad, Comonad<F> comonad, Defer<F> defer, java.util.stream.Stream<T> stream) {
    return from(monad, comonad, defer, ImmutableList.from(stream));
  }

  static <F extends Kind, T> Stream<F, T> from(Monad<F> monad, Comonad<F> comonad, Defer<F> defer, Sequence<T> sequence) {
    return sequence.foldLeft(Stream.<F, T>empty(monad, comonad, defer),
        (acc, a) -> acc.append(monad.pure(a)));
  }

  static <F extends Kind, T> Stream<F, T> iterate(Monad<F> monad, Comonad<F> comonad, Defer<F> defer, T seed, Operator1<T> generator) {
    return new Cons<>(monad, comonad, defer, monad.pure(seed),
        new Suspend<>(monad, defer, defer.defer(() -> monad.pure(iterate(monad, comonad, defer, generator.apply(seed), generator)))));
  }

  static <F extends Kind, T> Stream<F, T> iterate(Monad<F> monad, Comonad<F> comonad, Defer<F> defer, Producer<T> generator) {
    return new Cons<>(monad, comonad, defer, monad.pure(generator.get()),
        new Suspend<>(monad, defer, defer.defer(() -> monad.pure(iterate(monad, comonad, defer, generator)))));
  }

  static <F extends Kind, T> Stream<F, T> narrowK(Higher1<Higher1<Stream.µ, F>, T> hkt) {
    return (Stream<F, T>) hkt;
  }

  static <F extends Kind, T> Stream<F, T> narrowK(Higher2<Stream.µ, F, T> hkt) {
    return (Stream<F, T>) hkt;
  }
}

final class Cons<F extends Kind, T> implements Stream<F, T> {

  private final Monad<F> monad;
  private final Comonad<F> comonad;
  private final Defer<F> defer;
  private final Higher1<F, T> head;
  private final Stream<F, T> tail;

  Cons(Monad<F> monad, Comonad<F> comonad, Defer<F> defer, Higher1<F, T> head, Stream<F, T> tail) {
    this.monad = requireNonNull(monad);
    this.comonad = requireNonNull(comonad);
    this.defer = requireNonNull(defer);
    this.head = requireNonNull(head);
    this.tail = requireNonNull(tail);
  }

  @Override
  public Stream<F, T> head() {
    return take(1);
  }

  @Override
  public Stream<F, T> tail() {
    return tail;
  }

  @Override
  public Stream<F, T> concat(Stream<F, T> other) {
    return suspend(() -> cons(head, tail.concat(other)));
  }

  @Override
  public Stream<F, T> append(Higher1<F, T> other) {
    return suspend(() -> cons(head, tail.append(other)));
  }

  @Override
  public Stream<F, T> prepend(Higher1<F, T> other) {
    return suspend(() -> cons(other, tail.prepend(head)));
  }

  @Override
  public Stream<F, T> take(int n) {
    return n > 0 ? suspend(() -> cons(head, tail.take(n - 1))) : empty();
  }

  @Override
  public Stream<F, T> drop(int n) {
    return n > 0 ? suspend(() -> tail.drop(n - 1)) : this;
  }

  @Override
  public Stream<F, T> takeWhile(Matcher1<T> matcher) {
    return suspend(() -> comonad.extract(
        monad.map(head, t -> matcher.match(t) ?
            cons(head, tail.takeWhile(matcher)) : empty())));
  }

  @Override
  public Stream<F, T> dropWhile(Matcher1<T> matcher) {
    return suspend(() -> comonad.extract(
            monad.map(head, t -> matcher.match(t) ?
                tail.dropWhile(matcher) : this)));
  }

  @Override
  public Stream<F, T> filter(Matcher1<T> matcher) {
    return suspend(() -> comonad.extract(
            monad.map(head, t -> matcher.match(t) ?
                cons(head, tail.filter(matcher)) : tail.filter(matcher))));
  }

  @Override
  public <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator) {
    return monad.flatMap(head, h -> tail.foldLeft(combinator.apply(begin, h), combinator));
  }

  @Override
  public <R> Eval<Higher1<F, R>> foldRight(Eval<R> begin, Function2<T, Eval<R>, Eval<R>> combinator) {
    return later(() -> monad.flatMap(
        head, h -> tail.foldRight(combinator.apply(h, begin), combinator).value()));
  }

  @Override
  public <R> Stream<F, R> map(Function1<T, R> map) {
    return suspend(() -> cons(monad.map(head, map), suspend(() -> tail.map(map))));
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper) {
    return suspend(() -> cons(monad.flatMap(head, mapper), suspend(() -> tail.mapEval(mapper))));
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<Stream.µ, F, R>> map) {
    return suspend(() -> comonad.extract(
        monad.map(
            monad.map(head, map.andThen(Stream::narrowK)::apply),
            s -> s.concat(tail.flatMap(map)))));
  }

  @Override
  public Stream<F, T> repeat() {
    return concat(suspend(this::repeat));
  }

  @Override
  public Stream<F, T> intersperse(Higher1<F, T> value) {
    return suspend(() -> cons(head, suspend(() -> cons(value, tail.intersperse(value)))));
  }

  private <R> Stream<F, R> cons(Higher1<F, R> head, Stream<F, R> tail) {
    return new Cons<>(monad, comonad, defer, head, tail);
  }

  private <R> Stream<F, R> suspend(Producer<Stream<F, R>> stream) {
    return new Suspend<>(monad, defer, defer.defer(stream.andThen(monad::pure)));
  }

  private Stream<F, T> empty() {
    return Stream.empty(monad, comonad, defer);
  }
}

final class Suspend<F extends Kind, T> implements Stream<F, T> {

  private final Monad<F> monad;
  private final Defer<F> defer;
  private final Higher1<F, Stream<F, T>> evalStream;

  Suspend(Monad<F> monad, Defer<F> defer, Higher1<F, Stream<F, T>> stream) {
    this.monad = requireNonNull(monad);
    this.defer = requireNonNull(defer);
    this.evalStream = requireNonNull(stream);
  }

  @Override
  public Stream<F, T> head() {
    return lazyMap(Stream::head);
  }

  @Override
  public Stream<F, T> tail() {
    return lazyMap(Stream::tail);
  }

  @Override
  public Stream<F, T> concat(Stream<F, T> other) {
    return lazyMap(s -> s.concat(other));
  }

  @Override
  public Stream<F, T> append(Higher1<F, T> other) {
    return lazyMap(s -> s.append(other));
  }

  @Override
  public Stream<F, T> prepend(Higher1<F, T> other) {
    return lazyMap(s -> s.prepend(other));
  }

  @Override
  public Stream<F, T> take(int n) {
    return lazyMap(s -> s.take(n));
  }

  @Override
  public Stream<F, T> drop(int n) {
    return lazyMap(s -> s.drop(n));
  }

  @Override
  public Stream<F, T> takeWhile(Matcher1<T> matcher) {
    return lazyMap(s -> s.takeWhile(matcher));
  }

  @Override
  public Stream<F, T> dropWhile(Matcher1<T> matcher) {
    return lazyMap(s -> s.dropWhile(matcher));
  }

  @Override
  public Stream<F, T> filter(Matcher1<T> matcher) {
    return lazyMap(s -> s.filter(matcher));
  }

  @Override
  public <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator) {
    return monad.flatMap(evalStream, s -> s.foldLeft(begin, combinator));
  }

  @Override
  public <R> Eval<Higher1<F, R>> foldRight(Eval<R> begin, Function2<T, Eval<R>, Eval<R>> combinator) {
    return later(() -> monad.flatten(monad.map(evalStream, s -> s.foldRight(begin, combinator).value())));
  }

  @Override
  public <R> Stream<F, R> map(Function1<T, R> mapper) {
    return lazyMap(s -> s.map(mapper));
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper) {
    return suspend(() -> monad.map(evalStream, s -> s.mapEval(mapper)));
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<µ, F, R>> map) {
    return lazyMap(s -> s.flatMap(map));
  }

  @Override
  public Stream<F, T> repeat() {
    return lazyMap(s -> s.repeat());
  }

  @Override
  public Stream<F, T> intersperse(Higher1<F, T> value) {
    return lazyMap(s -> s.intersperse(value));
  }

  private <R> Stream<F, R> lazyMap(Function1<Stream<F, T>, Stream<F, R>> mapper) {
    return suspend(() -> monad.map(evalStream, mapper));
  }

  private <R> Stream<F, R> suspend(Producer<Higher1<F, Stream<F, R>>> stream) {
    return new Suspend<>(monad, defer, defer.defer(stream));
  }
}

final class Nil<F extends Kind, T> implements Stream<F, T> {

  private final Monad<F> monad;
  private final Comonad<F> comonad;
  private final Defer<F> defer;

  Nil(Monad<F> monad, Comonad<F> comonad, Defer<F> defer) {
    this.monad = requireNonNull(monad);
    this.comonad = requireNonNull(comonad);
    this.defer = requireNonNull(defer);
  }

  @Override
  public Stream<F, T> head() {
    return this;
  }

  @Override
  public Stream<F, T> tail() {
    return this;
  }

  @Override
  public Stream<F, T> take(int n) {
    return this;
  }

  @Override
  public Stream<F, T> drop(int n) {
    return this;
  }

  @Override
  public Stream<F, T> filter(Matcher1<T> matcher) {
    return this;
  }

  @Override
  public Stream<F, T> takeWhile(Matcher1<T> matcher) {
    return this;
  }

  @Override
  public Stream<F, T> dropWhile(Matcher1<T> matcher) {
    return this;
  }

  @Override
  public Stream<F, T> concat(Stream<F, T> other) {
    return other;
  }

  @Override
  public Stream<F, T> append(Higher1<F, T> other) {
    return new Cons<>(monad, comonad, defer, other, this);
  }

  @Override
  public Stream<F, T> prepend(Higher1<F, T> other) {
    return append(other);
  }

  @Override
  public <R> Higher1<F, R> foldLeft(R begin, Function2<R, T, R> combinator) {
    return monad.pure(begin);
  }

  @Override
  public <R> Eval<Higher1<F, R>> foldRight(Eval<R> begin, Function2<T, Eval<R>, Eval<R>> combinator) {
    return begin.map(monad::pure);
  }

  @Override
  public <R> Stream<F, R> map(Function1<T, R> map) {
    return new Nil<>(monad, comonad, defer);
  }

  @Override
  public <R> Stream<F, R> mapEval(Function1<T, Higher1<F, R>> mapper) {
    return new Nil<>(monad, comonad, defer);
  }

  @Override
  public <R> Stream<F, R> flatMap(Function1<T, ? extends Higher2<Stream.µ, F, R>> map) {
    return new Nil<>(monad, comonad, defer);
  }

  @Override
  public Stream<F, T> repeat() {
    return this;
  }

  @Override
  public Stream<F, T> intersperse(Higher1<F, T> value) {
    return this;
  }
}