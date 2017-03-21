package com.chenfei.exview.internal;

import java.io.Serializable;

public final class Exclusion implements Serializable {
  public final String name;
  public final String reason;
  public final String matching;

  public Exclusion(String matching, String name, String reason) {
    this.matching = matching;
    this.name = name;
    this.reason = reason;
  }
}
