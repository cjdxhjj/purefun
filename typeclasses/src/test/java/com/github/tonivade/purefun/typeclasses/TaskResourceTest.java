/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import org.junit.jupiter.api.Disabled;
import com.github.tonivade.purefun.effect.Task_;

@Disabled
public class TaskResourceTest extends ResourceTest<Task_> {

  public TaskResourceTest() {
    super(Task_.class);
  }
}