package io.github.anjoismysign.psa.crud;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.logging.Logger;

public interface CrudDatabase<T extends Crudable> {
  @Nullable
  Logger getLogger();
  
  @NotNull
  CrudManager<T> crudManagerOf(@NotNull Function<String, T> paramFunction);
}
