package me.anjoismysign.psa.crud;

import java.io.Serializable;
import org.jetbrains.annotations.NotNull;

public interface Crudable extends Serializable {
  @NotNull
  String getIdentification();
}
