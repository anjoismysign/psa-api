package me.anjoismysign.psa.lehmapp;

import java.util.Optional;
import me.anjoismysign.psa.crud.Crudable;
import org.jetbrains.annotations.NotNull;

public final class LehmappCrudable implements Crudable {
  private final String id;
  
  private final Lehmapp lehmapp;
  
  public LehmappCrudable(String id, Lehmapp lehmapp) {
    this.id = id;
    this.lehmapp = lehmapp;
  }
  
  public LehmappCrudable(String uuid) {
    this(uuid, new Lehmapp());
  }
  
  @NotNull
  public String getIdentification() {
    return this.id;
  }
  
  @NotNull
  public Lehmapp getLehmapp() {
    return this.lehmapp;
  }
  
  @NotNull
  public Optional<Object> has(String key) {
    return Optional.ofNullable(this.lehmapp.get(key));
  }
  
  @NotNull
  public Optional<Long> hasLong(String key) {
    return Optional.ofNullable(this.lehmapp.getLong(key));
  }
  
  @NotNull
  public Optional<Float> hasFloat(String key) {
    Float value = this.lehmapp.<Float>get(key, Float.class);
    if (value == null)
      return Optional.empty(); 
    return Optional.of(value);
  }
  
  @NotNull
  public Optional<Double> hasDouble(String key) {
    return Optional.ofNullable(this.lehmapp.getDouble(key));
  }
  
  @NotNull
  public Optional<Integer> hasInteger(String key) {
    return Optional.ofNullable(this.lehmapp.getInteger(key));
  }
  
  @NotNull
  public Optional<Short> hasShort(String key) {
    Short value = this.lehmapp.<Short>get(key, Short.class);
    if (value == null)
      return Optional.empty(); 
    return Optional.of(value);
  }
  
  @NotNull
  public Optional<Byte> hasByte(String key) {
    Byte value = this.lehmapp.<Byte>get(key, Byte.class);
    if (value == null)
      return Optional.empty(); 
    return Optional.of(value);
  }
  
  @NotNull
  public Optional<String> hasString(String key) {
    return Optional.ofNullable(this.lehmapp.getString(key));
  }
  
  @NotNull
  public Optional<Character> hasCharacter(String key) {
    Character value = this.lehmapp.<Character>get(key, Character.class);
    if (value == null)
      return Optional.empty(); 
    return Optional.of(value);
  }
  
  @NotNull
  public Optional<Boolean> hasBoolean(String key) {
    return Optional.ofNullable(this.lehmapp.getBoolean(key));
  }
}
