/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.OptionOf;
import com.github.tonivade.purefun.type.Option_;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.type.TryOf;
import com.github.tonivade.purefun.type.Try_;

public class FunctionKTest {

  @Test
  public void apply() {
    Higher1<Try_, String> success = new OptionToTry().apply(Option.some("hello world!"));

    assertEquals(Try.success("hello world!"), success);
  }

  @Test
  public void andThen() {
    Higher1<Option_, String> some = new OptionToTry().andThen(new TryToOption()).apply(Option.some("hello world!"));

    assertEquals(Option.some("hello world!"), some);
  }

  @Test
  public void compose() {
    Higher1<Try_, String> some = new OptionToTry().compose(new TryToOption()).apply(Try.success("hello world!"));

    assertEquals(Try.success("hello world!"), some);
  }
}

class OptionToTry implements FunctionK<Option_, Try_> {
  @Override
  public <X> Higher1<Try_, X> apply(Higher1<Option_, X> from) {
    return OptionOf.narrowK(from).map(Try::success).getOrElse(Try::failure);
  }
}

class TryToOption implements FunctionK<Try_, Option_> {
  @Override
  public <X> Higher1<Option_, X> apply(Higher1<Try_, X> from) {
    return TryOf.narrowK(from).toOption();
  }
}