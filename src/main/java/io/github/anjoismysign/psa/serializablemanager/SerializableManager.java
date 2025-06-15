package io.github.anjoismysign.psa.serializablemanager;

import io.github.anjoismysign.psa.lehmapp.LehmappCrudable;
import io.github.anjoismysign.psa.lehmapp.LehmappSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface SerializableManager<T extends LehmappSerializable> {
  @NotNull
  Function<LehmappCrudable, T> deserialize();
  
  @Nullable
  T cacheLook(@NotNull String paramString);
  
  void save(@NotNull T paramT);
  
  void syncSaveAll();
}
