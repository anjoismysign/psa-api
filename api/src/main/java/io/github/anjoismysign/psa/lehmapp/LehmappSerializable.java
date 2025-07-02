package io.github.anjoismysign.psa.lehmapp;

import org.jetbrains.annotations.NotNull;

public interface LehmappSerializable {
  @NotNull
  LehmappCrudable serialize();
}
