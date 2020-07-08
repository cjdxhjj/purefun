/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Witness;

public abstract class ReferenceTest<F extends Witness> {

  protected abstract <T> Reference<F, T> makeRef(T value);
  
  protected abstract <T, R> Kind<F, R> doAndThen(Kind<F, T> value, Kind<F, R> next);
  
  protected abstract <T> T run(Kind<F, T> value);

  @Test
  public void get() {
    Reference<F, String> ref = makeRef("Hello World!");

    Kind<F, String> result = ref.get();

    assertEquals("Hello World!", run(result));
  }


  @Test
  public void set() {
    Reference<F, String> ref = makeRef("Hello World!");

    Kind<F, String> result = doAndThen(ref.set("Something else"), ref.get());

    assertEquals("Something else", run(result));
  }

  @Test
  public void getAndSet() {
    Reference<F, String> ref = makeRef("Hello World!");

    Kind<F, String> result = ref.getAndSet("Something else");
    Kind<F, String> afterUpdate = doAndThen(result, ref.get());

    assertEquals("Hello World!", run(result));
    assertEquals("Something else", run(afterUpdate));
  }

  @Test
  public void getAndUpdate() {
    Reference<F, String> ref = makeRef("Hello World!");

    Kind<F, String> result = ref.getAndUpdate(String::toUpperCase);
    Kind<F, String> afterUpdate = doAndThen(result, ref.get());

    assertEquals("Hello World!", run(result));
    assertEquals("HELLO WORLD!", run(afterUpdate));
  }

  @Test
  public void updateAndGet() {
    Reference<F, String> ref = makeRef("Hello World!");

    Kind<F, String> result = ref.updateAndGet(String::toUpperCase);

    assertEquals("HELLO WORLD!", run(result));
  }
}
