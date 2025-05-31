package me.anjoismysign.psa.crud.controller;

import java.util.function.Function;
import me.anjoismysign.psa.crud.CrudDatabase;
import me.anjoismysign.psa.crud.CrudManager;
import org.jetbrains.annotations.NotNull;

public interface DatabaseCredentialsController<T extends me.anjoismysign.psa.crud.Crudable> {
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