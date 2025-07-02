package io.github.anjoismysign.psa.crud.controller;

import io.github.anjoismysign.psa.crud.CrudDatabase;
import io.github.anjoismysign.psa.crud.CrudManager;
import io.github.anjoismysign.psa.lehmapp.LehmappCrudable;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record LehmappDatabaseCredentialsController(@NotNull CrudDatabase<LehmappCrudable> getCredentials,
                                                   @NotNull Function<String, LehmappCrudable> getCreateFunction,
                                                   @NotNull CrudManager<LehmappCrudable> getCrudManager) implements DatabaseCredentialsController<LehmappCrudable> {
  
  public LehmappDatabaseCredentialsController(@NotNull CrudDatabase<LehmappCrudable> getCredentials, @NotNull Function<String, LehmappCrudable> getCreateFunction, @NotNull CrudManager<LehmappCrudable> getCrudManager) {
    this.getCredentials = getCredentials;
    this.getCreateFunction = getCreateFunction;
    this.getCrudManager = getCrudManager;
  }
  
  public static LehmappDatabaseCredentialsController of(@NotNull CrudDatabase<LehmappCrudable> credentials, @NotNull Function<String, LehmappCrudable> createFunction) {
    return new LehmappDatabaseCredentialsController(credentials, createFunction, credentials.crudManagerOf(createFunction));
  }
}
