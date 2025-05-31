package me.anjoismysign.psa.crud;

import java.util.function.Function;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CrudDatabase {
  @Nullable
  Logger getLogger();
  
  @NotNull
  <T extends Crudable> CrudManager<T> crudManagerOf(@NotNull Function<String, T> paramFunction);
}
