package me.anjoismysign.psa.lehmapp;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Lehmapp implements Map<String, Object>, Serializable {
  private static final long serialVersionUID = 7828118618100017483L;
  
  private final LinkedHashMap<String, Object> asMap = new LinkedHashMap<>();
  
  public int size() {
    return this.asMap.size();
  }
  
  public boolean isEmpty() {
    return this.asMap.isEmpty();
  }
  
  public boolean containsValue(Object value) {
    return this.asMap.containsValue(value);
  }
  
  public boolean containsKey(Object key) {
    return this.asMap.containsKey(key);
  }
  
  @Nullable
  public Object get(Object key) {
    return this.asMap.get(key);
  }
  
  @Nullable
  public Object put(String key, Object value) {
    return this.asMap.put(key, value);
  }
  
  @Nullable
  public Object remove(Object key) {
    return this.asMap.remove(key);
  }
  
  public void putAll(@NotNull Map<? extends String, ?> map) {
    this.asMap.putAll(map);
  }
  
  public void clear() {
    this.asMap.clear();
  }
  
  @NotNull
  public Set<String> keySet() {
    return this.asMap.keySet();
  }
  
  @NotNull
  public Collection<Object> values() {
    return this.asMap.values();
  }
  
  @NotNull
  public Set<Entry<String, Object>> entrySet() {
    return this.asMap.entrySet();
  }
  
  public boolean equals(Object object) {
    if (this == object)
      return true; 
    if (object == null || getClass() != object.getClass())
      return false; 
    Lehmapp skechers = (Lehmapp)object;
    return this.asMap.equals(skechers.asMap);
  }
  
  public int hashCode() {
    return this.asMap.hashCode();
  }
  
  @NotNull
  public String toString() {
    return simpleName() + "{" + simpleName() + "}";
  }
  
  @NotNull
  private String simpleName() {
    return getClass().getSimpleName();
  }
  
  @Nullable
  public Integer getInteger(Object key) {
    Object object = get(key);
    if (object instanceof Integer) {
      Integer result = (Integer)object;
      return result;
    } 
    return null;
  }
  
  public Long getLong(Object key) {
    Object object = get(key);
    if (object instanceof Long) {
      Long result = (Long)object;
      return result;
    } 
    return null;
  }
  
  public Double getDouble(Object key) {
    Object object = get(key);
    if (object instanceof Double) {
      Double result = (Double)object;
      return result;
    } 
    return null;
  }
  
  public String getString(Object key) {
    Object object = get(key);
    if (object instanceof String) {
      String result = (String)object;
      return result;
    } 
    return null;
  }
  
  public Boolean getBoolean(Object key) {
    Object object = get(key);
    if (object instanceof Boolean) {
      Boolean result = (Boolean)object;
      return result;
    } 
    return null;
  }
  
  public Date getDate(Object key) {
    Object object = get(key);
    if (object instanceof Date) {
      Date result = (Date)object;
      return result;
    } 
    return null;
  }
  
  public <T> T get(String key, Class<T> clazz) {
    Objects.requireNonNull(key, "'key' cannot be null");
    Objects.requireNonNull(clazz, "'clazz' cannot be null");
    this.asMap.get(key);
    return clazz.cast(this.asMap.get(key));
  }
}
