package io.github.anjoismysign.psa.crud;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public interface Crudable extends Serializable {
  @NotNull
  String getIdentification();
}
