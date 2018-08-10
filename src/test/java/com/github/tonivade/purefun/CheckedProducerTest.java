/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.github.tonivade.purefun.Nothing.nothing;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class CheckedProducerTest {

  @Test
  public void andThen() throws Exception {
    CheckedProducer<String> producer = CheckedProducer.of(() -> "hello world");
    
    CheckedProducer<String> andThen = producer.andThen(String::toUpperCase);
    
    assertEquals("HELLO WORLD", andThen.get());
  }
  
  @Test
  public void unit() throws Exception {
    assertEquals("hello world", CheckedProducer.unit("hello world").get());
  }

  @Test
  public void asFunction() throws Exception {
    CheckedProducer<String> producer = CheckedProducer.unit("hello world");

    assertEquals("hello world", producer.asFunction().apply(nothing()));
  }
}
