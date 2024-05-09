/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import static com.github.tonivade.purefun.core.Function1.identity;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.core.Operator1;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.typeclasses.Instances;

public class CoyonedaTest {

  private final Operator1<String> concat = string -> string.concat(string);

  @Test
  public void coyoneda() {
    Coyoneda<Option<?>, String, String> coyoneda = Coyoneda.of(Option.some("string"), identity());

    Coyoneda<Option<?>, String, String> result = coyoneda.map(concat).map(concat);

    assertEquals(Option.some("stringstringstringstring"), result.run(Instances.functor()));
  }
}
