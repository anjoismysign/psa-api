package me.anjoismysign.psa.serializablemanager;

import me.anjoismysign.psa.lehmapp.LehmappSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SerializableManagerContainer {
  @Nullable
  <T extends LehmappSerializable> SerializableManager<T> getSerializableManager(@NotNull Class<T> paramClass);
}
