package me.anjoismysign.psa.crud;

import java.util.function.Function;
import me.anjoismysign.psa.lehmapp.LehmappCrudable;
import me.anjoismysign.psa.lehmapp.LehmappSerializable;
import me.anjoismysign.psa.serializablemanager.SerializableManager;
import org.jetbrains.annotations.NotNull;

public interface CrudDatabaseCredentials extends DatabaseCredentials {
  @NotNull
  <T extends Crudable> CrudDatabase getCrudDatabaseFor(@NotNull Class<T> paramClass);
  
  @NotNull
  <T extends LehmappSerializable> SerializableManager<T> getSerializableManager(@NotNull Class<T> paramClass, @NotNull Function<LehmappCrudable, T> paramFunction);
}
