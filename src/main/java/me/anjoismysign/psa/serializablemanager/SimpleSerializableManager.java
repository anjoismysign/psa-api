package me.anjoismysign.psa.serializablemanager;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import me.anjoismysign.psa.crud.CrudDatabaseCredentials;
import me.anjoismysign.psa.crud.controller.DatabaseCredentialsController;
import me.anjoismysign.psa.crud.controller.LehmappDatabaseCredentialsController;
import me.anjoismysign.psa.lehmapp.LehmappCrudable;
import me.anjoismysign.psa.lehmapp.LehmappSerializable;
import org.jetbrains.annotations.NotNull;

public class SimpleSerializableManager<T extends LehmappSerializable> implements SerializableManager<T> {
  protected final CrudDatabaseCredentials databaseCredentials;
  
  protected final DatabaseCredentialsController<LehmappCrudable> controller;
  
  protected final Function<LehmappCrudable, T> deserializeFunction;
  
  private final Map<String, T> map = new HashMap<>();
  
  public SimpleSerializableManager(@NotNull CrudDatabaseCredentials credentials, @NotNull Function<LehmappCrudable, T> deserializeFunction) {
    this.databaseCredentials = credentials;
    this.controller = (DatabaseCredentialsController<LehmappCrudable>)LehmappDatabaseCredentialsController.of(credentials.getCrudDatabaseFor(LehmappCrudable.class), LehmappCrudable::new);
    this.deserializeFunction = deserializeFunction;
  }
  
  @NotNull
  public Map<String, T> map() {
    return this.map;
  }
  
  @NotNull
  public T cacheLook(@NotNull String identification) {
    return this.map.get(identification);
  }
  
  public void save(@NotNull T serializable) {
    this.controller.getCrudManager().update(serializable.serialize());
  }
  
  public void syncSaveAll() {
    this.map.values().forEach(serializable -> this.controller.getCrudManager().update(serializable.serialize()));
  }
  
  @NotNull
  public Function<LehmappCrudable, T> deserialize() {
    return this.deserializeFunction;
  }
}
