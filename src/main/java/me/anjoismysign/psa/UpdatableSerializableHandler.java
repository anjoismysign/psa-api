package me.anjoismysign.psa;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public record UpdatableSerializableHandler<T extends Serializable>(@NotNull T value,
                                                                   int version) implements UpdatableSerializable<T> {
  
  public UpdatableSerializableHandler(T value, int version) {
    this.value = value;
    this.version = version;
  }
  
  public static <T extends Serializable> UpdatableSerializableHandler<T> of(T value, int version) {
    return new UpdatableSerializableHandler<>(value, version);
  }
  
  public int getVersion() {
    return this.version;
  }

  public void setVersion(int version) {}
  
  public T getValue() {
    return this.value;
  }

  public void setValue(T value) {}
}
