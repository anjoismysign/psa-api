package me.anjoismysign.psa.serializablemanager;

import java.util.function.Function;
import me.anjoismysign.psa.lehmapp.LehmappCrudable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SerializableManager<T extends me.anjoismysign.psa.lehmapp.LehmappSerializable> {
  @NotNull
  Function<LehmappCrudable, T> deserialize();
  
  @Nullable
  T cacheLook(@NotNull String paramString);
  
  void save(@NotNull T paramT);
  
  void syncSaveAll();
}
