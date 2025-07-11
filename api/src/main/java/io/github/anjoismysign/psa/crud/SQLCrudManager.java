package io.github.anjoismysign.psa.crud;

import java.sql.Connection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface SQLCrudManager<T extends Crudable> extends CrudManager<T> {
  String getCrudableKeyTypeName();
  
  String getTableName();
  
  String getPrimaryKeyName();
  
  default String crudableKeyTypePrepareStatement() {
    return getCrudableKeyTypeName() + "=?";
  }
  
  int getPrimaryKeyLength();
  
  Connection getConnection();
  
  boolean exists(String paramString);
  
  default void forEachRecord(Consumer<T> consumer) {
    forEachRecord((crudable, version) -> consumer.accept(crudable));
  }
  
  void forEachRecord(BiConsumer<T, Integer> paramBiConsumer);
  
  void reload();
}
