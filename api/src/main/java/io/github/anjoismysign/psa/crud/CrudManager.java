package io.github.anjoismysign.psa.crud;

import io.github.anjoismysign.psa.UpdatableSerializableHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.UUID;
import java.util.logging.Logger;

public interface CrudManager<T extends Serializable> {
  @NotNull
  default T create() {
    return create(UUID.randomUUID().toString());
  }
  
  boolean exists(String paramString);
  
  @NotNull
  T create(String paramString);
  
  @NotNull
  T read(String paramString);
  
  @Nullable
  T readOrNull(String paramString);
  
  void update(T paramT);
  
  void delete(String paramString);
  
  @Nullable
  Logger getLogger();
  
  default UpdatableSerializableHandler<T> newUpdatable(T value, int version) {
    return new UpdatableSerializableHandler((Serializable)value, version);
  }
}
