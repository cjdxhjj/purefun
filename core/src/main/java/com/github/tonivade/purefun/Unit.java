/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import java.io.Serializable;

/**
 * Type that represents a single value called Unit.
 */
public final class Unit implements Serializable {

  private static final long serialVersionUID = -8253613036328680583L;

  private static final Unit INSTANCE = new Unit();

  private Unit() {}

  public static Unit unit() {
    return INSTANCE;
  }

  @Override
  public String toString() {
    return "Unit";
  }
}
