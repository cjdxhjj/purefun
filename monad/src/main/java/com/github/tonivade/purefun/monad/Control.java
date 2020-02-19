/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.monad;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Recoverable;
import com.github.tonivade.purefun.Sealed;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.ImmutableMap;
import com.github.tonivade.purefun.data.NonEmptyList;
import com.github.tonivade.purefun.type.Option;

import static com.github.tonivade.purefun.Producer.cons;
import static com.github.tonivade.purefun.Unit.unit;
import static java.util.Objects.requireNonNull;

@Sealed
@HigherKind
public interface Control<T> {

  <R> Result<R> apply(Cont<T, R> cont);

  default T run() {
    return Result.trampoline(apply(Cont._return()));
  }

  default <R> Control<R> map(Function1<T, R> mapper) {
    return new Control<R>() {
      @Override
      public <R1> Result<R1> apply(Cont<R, R1> cont) {
        return Control.this.apply(cont.map(mapper));
      }
    };
  }

  default <R> Control<R> flatMap(Function1<T, Control<R>> mapper) {
    return new Control<R>() {
      @Override
      public <R1> Result<R1> apply(Cont<R, R1> cont) {
        return Control.this.apply(cont.flatMap(mapper));
      }
    };
  }

  default <R> Control<R> andThen(Control<R> next) {
    return flatMap(ignore -> next);
  }

  static <T> Control<T> pure(T value) {
    return later(cons(value));
  }

  static <T> Control<T> later(Producer<T> value) {
    return new Pure<>(value);
  }

  static <T> Control<T> failure(Throwable throwable) {
    return new Failure<>(throwable);
  }

  static <T, R> Control<T> use(Marker.Cont<R> marker, Function1<Function1<T, Control<R>>, Control<R>> cps) {
    return new Use<>(marker, cps);
  }

  static <R> Control<R> delimitCont(Marker.Cont<R> marker, Function1<? super Marker.Cont<R>, Control<R>> control) {
    return new DelimitCont<>(marker, control);
  }

  static <R, S> Control<R> delimitState(Marker.State<S> marker, Control<R> control) {
    return new DelimitState<>(marker, control);
  }

  static <R> Control<R> delimitCatch(Marker.Catch<R> marker, Control<R> control) {
    return new DelimitCatch<>(marker, control);
  }

  final class Use<A, R> implements Control<A> {

    private final Marker.Cont<R> marker;
    private final Function1<Function1<A, Control<R>>, Control<R>> cps;

    private Use(Marker.Cont<R> marker, Function1<Function1<A, Control<R>>, Control<R>> cps) {
      this.marker = requireNonNull(marker);
      this.cps = requireNonNull(cps);
    }

    @Override
    public <R1> Result<R1> apply(Cont<A, R1> cont) {
      Tuple2<Cont<A, R>, Cont<R, R1>> tuple = cont.splitAt(marker);
      Control<R> handled = cps.apply(value -> new Control<R>() {
        @Override
        public <R2> Result<R2> apply(Cont<R, R2> cont) {
          return tuple.get1().append(cont).apply(value);
        }
      });
      return Result.computation(handled, tuple.get2());
    }
  }

  final class DelimitCont<R> implements Control<R> {

    private final Marker.Cont<R> marker;
    private final Function1<? super Marker.Cont<R>, Control<R>> control;

    private DelimitCont(Marker.Cont<R> marker, Function1<? super Marker.Cont<R>, Control<R>> control) {
      this.marker = requireNonNull(marker);
      this.control = requireNonNull(control);
    }

    @Override
    public <R1> Result<R1> apply(Cont<R, R1> cont) {
      return Result.computation(control.apply(marker), Cont.handler(marker, cont));
    }
  }

  final class DelimitState<R, S> implements Control<R> {

    private final Marker.State<S> marker;
    private final Control<R> control;

    private DelimitState(Marker.State<S> marker, Control<R> control) {
      this.marker = requireNonNull(marker);
      this.control = requireNonNull(control);
    }

    @Override
    public <R1> Result<R1> apply(Cont<R, R1> cont) {
      return Result.computation(control, Cont.state(marker, cont));
    }
  }

  final class DelimitCatch<R> implements Control<R> {

    private final Marker.Catch<R> marker;
    private final Control<R> control;

    private DelimitCatch(Marker.Catch<R> marker, Control<R> control) {
      this.marker = requireNonNull(marker);
      this.control = requireNonNull(control);
    }

    @Override
    public <R1> Result<R1> apply(Cont<R, R1> cont) {
      return Result.computation(control, Cont._catch(marker, cont));
    }
  }

  final class Pure<T> implements Control<T> {

    private final Producer<T> value;

    private Pure(Producer<T> value) {
      this.value = requireNonNull(value);
    }

    @Override
    public T run() {
      return value.get();
    }

    @Override
    public <R> Result<R> apply(Cont<T, R> cont) {
      return cont.apply(value.get());
    }

    @Override
    public <R> Control<R> map(Function1<T, R> mapper) {
      return new Pure<>(() -> mapper.apply(value.get()));
    }
  }

  @SuppressWarnings("unchecked")
  final class Failure<T> implements Control<T> {

    private final Throwable error;

    private Failure(Throwable error) {
      this.error = requireNonNull(error);
    }

    @Override
    public <R> Result<R> apply(Cont<T, R> cont) {
      return Result.abort(error);
    }

    @Override
    public <R> Control<R> map(Function1<T, R> mapper) {
      return (Control<R>) this;
    }

    @Override
    public <R> Control<R> flatMap(Function1<T, Control<R>> mapper) {
      return (Control<R>) this;
    }
  }

  interface Handler<R, E> extends Marker.Cont<R> {

    default <T> Control<T> use(Function1<Function1<T, Control<R>>, Control<R>> body) {
      return Control.use(this, body);
    }

    @SuppressWarnings("unchecked")
    default Control<R> apply(Function1<E, Control<R>> program) {
      if (this instanceof StateMarker) {
        return Control.delimitState((Marker.State<?>) this,
            Control.delimitCont(this, h -> program.apply((E) this)));
      }
      return Control.delimitCont(this, h -> program.apply((E) this));
    }
  }

  class Stateful<R, S, E> extends StateMarker implements Handler<R, E> {

    private final Field<S> state;

    public Stateful(S init) {
      this.state = field(init);
    }

    public <T> Control<T> useState(Function1<S, Function1<Function1<T, Function1<S, Control<R>>>, Control<R>>> body) {
      return this.use(resume ->
          state.get().flatMap(before ->
              body.apply(before).apply(value -> after -> state.set(after).andThen(resume.apply(value)))
          )
      );
    }
  }
}

@Sealed
interface Result<T> {

  static <T> T trampoline(Result<T> apply) {
    Result<T> result = apply;

    while (result.isComputation()) {
      Computation<T, ?> current = (Computation<T, ?>) result;

      try {
        result = current.apply();
      } catch (Throwable t) {
        result = current.unwind(t);
      }
    }

    return result.value();
  }

  T value();

  default boolean isComputation() {
    return false;
  }

  static <T> Result<T> value(T value) {
    return new Value<>(value);
  }

  static <T> Result<T> abort(Throwable error) {
    return new Abort<>(error);
  }

  static <T, R> Result<T> computation(Control<R> control, Cont<R, T> cont) {
    return new Computation<>(control, cont);
  }

  final class Value<T> implements Result<T> {

    private final T value;

    private Value(T value) {
      this.value = requireNonNull(value);
    }

    @Override
    public T value() {
      return value;
    }

    @Override
    public String toString() {
      return String.format("Value(%s)", value);
    }
  }

  final class Abort<T> implements Result<T>, Recoverable {

    private final Throwable error;

    private Abort(Throwable error) {
      this.error = requireNonNull(error);
    }

    @Override
    public T value() {
      return sneakyThrow(error);
    }

    @Override
    public String toString() {
      return String.format("Error(%s)", error);
    }
  }

  final class Computation<T, R> implements Result<T> {

    private final Control<R> control;
    private final Cont<R, T> continuation;

    private Computation(Control<R> control, Cont<R, T> continuation) {
      this.control = requireNonNull(control);
      this.continuation = requireNonNull(continuation);
    }

    @Override
    public boolean isComputation() {
      return true;
    }

    @Override
    public T value() {
      throw new UnsupportedOperationException();
    }

    public Result<T> apply() {
      return control.apply(continuation);
    }

    public Result<T> unwind(Throwable throwable) {
      return continuation.unwind(throwable);
    }

    @Override
    public String toString() {
      return String.format("Computation(%s, %s)", control, continuation);
    }
  }
}

@Sealed
interface Cont<A, B> {

  Result<B> apply(A value);

  <C> Cont<A, C> append(Cont<B, C> next);

  <R> Tuple2<Cont<A, R>, Cont<R, B>> splitAt(Marker.Cont<R> cont);

  default <R> Cont<R, B> map(Function1<R, A> mapper) {
    return flatMap(x -> Control.later(() -> mapper.apply(x)));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  default <R> Cont<R, B> flatMap(Function1<R, Control<A>> mapper) {
    Function1<?, Control<?>> f = (Function1) mapper;
    return new Frames<>(NonEmptyList.of(f), this);
  }

  Result<B> unwind(Throwable throwable);

  static <A> Cont<A, A> _return() {
    return new Return<>();
  }

  static <A, B, S> Cont<A, B> state(Marker.State<S> marker, Cont<A, B> tail) {
    return new State<>(marker, tail);
  }

  static <A, B> Cont<A, B> handler(Marker.Cont<A> marker, Cont<A, B> tail) {
    return new Handler<>(marker, tail);
  }

  static <A, B> Catch<A, B> _catch(Marker.Catch<A> marker, Cont<A, B> tail) {
    return new Catch<>(marker, tail);
  }

  final class Return<A> implements Cont<A, A>, Recoverable {

    private Return() {}

    @Override
    public Result<A> apply(A value) {
      return Result.value(value);
    }

    @Override
    public <C> Cont<A, C> append(Cont<A, C> next) {
      return next;
    }

    @Override
    public <R> Tuple2<Cont<A, R>, Cont<R, A>> splitAt(Marker.Cont<R> cont) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Result<A> unwind(Throwable throwable) {
      return sneakyThrow(throwable);
    }

    @Override
    public String toString() {
      return "[]";
    }
  }

  final class Frames<A, B, C> implements Cont<A, C> {

    private final NonEmptyList<Function1<?, Control<?>>> frames;
    private final Cont<B, C> tail;

    private Frames(NonEmptyList<Function1<?, Control<?>>> frames, Cont<B, C> tail) {
      this.frames = requireNonNull(frames);
      this.tail = requireNonNull(tail);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Result<C> apply(A value) {
      Option<Function1<?, Control<?>>> head = frames.head();
      ImmutableList<Function1<?, Control<?>>> rest = frames.tail();
      Function1<A, Control<B>> result = (Function1) head.get();
      if (rest.isEmpty()) {
        return Result.computation(result.apply(value), tail);
      }
      return Result.computation(result.apply(value), new Frames<>(NonEmptyList.of(rest), tail));
    }

    @Override
    public <D> Cont<A, D> append(Cont<C, D> next) {
      return new Frames<>(frames, tail.append(next));
    }

    @Override
    public <R> Tuple2<Cont<A, R>, Cont<R, C>> splitAt(Marker.Cont<R> cont) {
      return tail.splitAt(cont).applyTo((head, tail) -> Tuple2.of(new Frames<>(frames, head), tail));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R> Cont<R, C> flatMap(Function1<R, Control<A>> mapper) {
      NonEmptyList<Function1<?, Control<?>>> list = NonEmptyList.of((Function1) mapper);
      return new Frames<>(list.appendAll(frames), tail);
    }

    @Override
    public Result<C> unwind(Throwable throwable) {
      return tail.unwind(throwable);
    }

    @Override
    public String toString() {
      return String.format("frames :: %s", tail);
    }
  }

  final class Handler<R, A> implements Cont<R, A> {

    private final Marker.Cont<R> marker;
    private final Cont<R, A> tail;

    private Handler(Marker.Cont<R> marker, Cont<R, A> tail) {
      this.marker = requireNonNull(marker);
      this.tail = requireNonNull(tail);
    }

    @Override
    public Result<A> apply(R value) {
      return tail.apply(value);
    }

    @Override
    public <C> Cont<R, C> append(Cont<A, C> next) {
      return new Handler<>(marker, tail.append(next));
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <R1> Tuple2<Cont<R, R1>, Cont<R1, A>> splitAt(Marker.Cont<R1> cont) {
      if (cont.getClass().isAssignableFrom(marker.getClass())) {
        Handler<R, R1> handler = new Handler<>(marker, new Return());
        // XXX: does not match, don't now how it compiles on scala
        Tuple2<Cont<R, R1>, Cont<R, A>> tuple = Tuple2.of(handler, tail);
        return (Tuple2) tuple;
      }
      return tail.splitAt(cont).applyTo((head, tail) -> Tuple2.of(new Handler<>(marker, head), tail));
    }

    @Override
    public Result<A> unwind(Throwable throwable) {
      return tail.unwind(throwable);
    }

    @Override
    public String toString() {
      return String.format("prompt %s :: %s", marker, tail);
    }
  }

  final class State<R, S, A> implements Cont<R, A> {

    private final Marker.State<S> marker;
    private final Cont<R, A> tail;

    private State(Marker.State<S> marker, Cont<R, A> tail) {
      this.marker = requireNonNull(marker);
      this.tail = requireNonNull(tail);
    }

    @Override
    public Result<A> apply(R value) {
      return tail.apply(value);
    }

    @Override
    public <C> Cont<R, C> append(Cont<A, C> next) {
      return new State<>(marker, tail.append(next));
    }

    @Override
    public <R1> Tuple2<Cont<R, R1>, Cont<R1, A>> splitAt(Marker.Cont<R1> cont) {
      Tuple2<Cont<R, R1>, Cont<R1, A>> tuple = tail.splitAt(cont);
      return tuple.applyTo((head, tail) -> Tuple2.of(new Captured<>(marker, marker.backup(), head), tail));
    }

    @Override
    public Result<A> unwind(Throwable throwable) {
      return tail.unwind(throwable);
    }

    @Override
    public String toString() {
      return String.format("state %s :: %s", marker, tail);
    }
  }

  final class Catch<R, A> implements Cont<R, A> {

    private final Marker.Catch<R> marker;
    private final Cont<R, A> tail;

    private Catch(Marker.Catch<R> marker, Cont<R, A> tail) {
      this.marker = requireNonNull(marker);
      this.tail = requireNonNull(tail);
    }

    @Override
    public Result<A> apply(R value) {
      return tail.apply(value);
    }

    @Override
    public <C> Cont<R, C> append(Cont<A, C> next) {
      return new Catch<>(marker, tail.append(next));
    }

    @Override
    public <R1> Tuple2<Cont<R, R1>, Cont<R1, A>> splitAt(Marker.Cont<R1> cont) {
      Tuple2<Cont<R, R1>, Cont<R1, A>> tuple = tail.splitAt(cont);
      return tuple.applyTo((head, tail) -> Tuple2.of(new Catch<>(marker, head), tail));
    }

    @Override
    public Result<A> unwind(Throwable throwable) {
      if (marker.handle().isDefinedAt(throwable)) {
        return marker.handle().apply(throwable).apply(tail);
      }
      return tail.unwind(throwable);
    }

    @Override
    public String toString() {
      return String.format("catch %s :: %s", marker, tail);
    }
  }

  final class Captured<R, S, A> implements Cont<R, A> {

    private final Marker.State<S> marker;
    private final S state;
    private final Cont<R, A> tail;

    private Captured(Marker.State<S> marker, S state, Cont<R, A> tail) {
      this.marker = requireNonNull(marker);
      this.state = requireNonNull(state);
      this.tail = requireNonNull(tail);
    }

    @Override
    public Result<A> apply(R value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <C> Cont<R, C> append(Cont<A, C> next) {
      return new State<>(restore(), tail.append(next));
    }

    @Override
    public <R1> Tuple2<Cont<R, R1>, Cont<R1, A>> splitAt(Marker.Cont<R1> cont) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Result<A> unwind(Throwable throwable) {
      throw new UnsupportedOperationException();
    }

    private Marker.State<S> restore() {
      marker.restore(state);
      return marker;
    }

    @Override
    public String toString() {
      return "???";
    }
  }
}

interface Marker {

  interface Cont<R> {}

  interface State<S> {
    S backup();
    void restore(S value);
  }

  interface Catch<R> {
    PartialFunction1<Throwable, Control<R>> handle();
  }
}

class StateMarker implements Marker.State<ImmutableMap<StateMarker.Field<?>, Object>> {

  private ImmutableMap<Field<?>, Object> data = ImmutableMap.empty();

  @Override
  public ImmutableMap<Field<?>, Object> backup() {
    return data;
  }

  @Override
  public void restore(ImmutableMap<Field<?>, Object> value) {
    this.data = requireNonNull(value);
  }

  public <T> Field<T> field(T value) {
    Field<T> field = new Field<>();
    data = data.put(field, value);
    return field;
  }

  final class Field<T> {

    @SuppressWarnings("unchecked")
    public Control<T> get() {
      return Control.later(() -> (T) data.get(this).get());
    }

    public Control<Unit> set(T value) {
      return Control.later(() -> {
        data = data.put(this, value);
        return unit();
      });
    }

    public Control<Unit> update(Operator1<T> mapper) {
      return get().flatMap(x -> set(mapper.apply(x)));
    }
  }
}
