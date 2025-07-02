package io.github.anjoismysign.psa.crud.controller;

import io.github.anjoismysign.psa.crud.CrudDatabase;
import io.github.anjoismysign.psa.crud.CrudManager;
import io.github.anjoismysign.psa.crud.Crudable;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public interface DatabaseCredentialsController<T extends Crudable> {
  @NotNull
  CrudDatabase getCredentials();
  
  @NotNull
  CrudManager<T> getCrudManager();
  
  @NotNull
  Function<String, T> getCreateFunction();
}


/* Location:              /Users/luisbenavides-naranjo/Documents/Java/bloblib-1.698.31.jar!/me/anjoismysign/psa/crud/controller/DatabaseCredentialsController.class
 * Java compiler version: 16 (60.0)
 * JD-Core Version:       1.1.3
 */