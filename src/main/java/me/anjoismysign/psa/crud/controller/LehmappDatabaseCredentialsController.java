package me.anjoismysign.psa.crud.controller;

import java.util.function.Function;
import me.anjoismysign.psa.crud.CrudDatabase;
import me.anjoismysign.psa.crud.CrudManager;
import me.anjoismysign.psa.lehmapp.LehmappCrudable;
import org.jetbrains.annotations.NotNull;

public record LehmappDatabaseCredentialsController(@NotNull CrudDatabase getCredentials,
                                                   @NotNull Function<String, LehmappCrudable> getCreateFunction,
                                                   @NotNull CrudManager<LehmappCrudable> getCrudManager) implements DatabaseCredentialsController<LehmappCrudable> {
  
  public LehmappDatabaseCredentialsController(@NotNull CrudDatabase getCredentials, @NotNull Function<String, LehmappCrudable> getCreateFunction, @NotNull CrudManager<LehmappCrudable> getCrudManager) {
    this.getCredentials = getCredentials;
    this.getCreateFunction = getCreateFunction;
    this.getCrudManager = getCrudManager;
  }
  
  public static LehmappDatabaseCredentialsController of(@NotNull CrudDatabase credentials, @NotNull Function<String, LehmappCrudable> createFunction) {
    return new LehmappDatabaseCredentialsController(credentials, createFunction, credentials.crudManagerOf(createFunction));
  }
}
