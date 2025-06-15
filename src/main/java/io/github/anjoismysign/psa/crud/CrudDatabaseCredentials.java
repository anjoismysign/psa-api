package io.github.anjoismysign.psa.crud;

import io.github.anjoismysign.psa.lehmapp.LehmappCrudable;
import io.github.anjoismysign.psa.lehmapp.LehmappSerializable;
import io.github.anjoismysign.psa.serializablemanager.SerializableManager;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public interface CrudDatabaseCredentials extends DatabaseCredentials {
  @NotNull
  <T extends Crudable> CrudDatabase getCrudDatabaseFor(@NotNull Class<T> paramClass);
  
  @NotNull
  <T extends LehmappSerializable> SerializableManager<T> getSerializableManager(@NotNull Class<T> paramClass, @NotNull Function<LehmappCrudable, T> paramFunction);
}
