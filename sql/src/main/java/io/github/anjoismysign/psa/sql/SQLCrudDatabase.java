package io.github.anjoismysign.psa.sql;

import io.github.anjoismysign.psa.crud.CrudDatabase;
import io.github.anjoismysign.psa.crud.Crudable;
import org.jetbrains.annotations.NotNull;

public interface SQLCrudDatabase<T extends Crudable> extends CrudDatabase<T> {
    @NotNull
    SQLContainer generateContainer();
}
