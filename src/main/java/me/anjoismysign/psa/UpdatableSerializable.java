package me.anjoismysign.psa;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Optional;
import javax.sql.rowset.serial.SerialBlob;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface UpdatableSerializable<T extends Serializable> extends Serializable {
  static byte[] serialize(UpdatableSerializable updatableSerializable) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      ObjectOutput objectOutput = new ObjectOutputStream(byteArrayOutputStream);
      objectOutput.writeObject(updatableSerializable);
      byte[] bytes = byteArrayOutputStream.toByteArray();
      objectOutput.close();
      byteArrayOutputStream.close();
      return bytes;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    } 
  }
  
  @Nullable
  static SerialBlob blobSerialize(UpdatableSerializable updatableSerializable) {
    byte[] bytes = serialize(updatableSerializable);
    try {
      SerialBlob serialBlob = new SerialBlob(bytes);
      return serialBlob;
    } catch (SQLException e) {
      e.printStackTrace();
      return null;
    } 
  }
  
  @Nullable
  static UpdatableSerializable deserialize(byte[] bytes) {
    if (bytes == null)
      return null; 
    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
    try {
      ObjectInput objectInput = new ObjectInputStream(byteArrayInputStream);
      UpdatableSerializable updatableSerializable = (UpdatableSerializable)objectInput.readObject();
      return updatableSerializable;
    } catch (IOException|ClassNotFoundException e) {
      e.printStackTrace();
      return null;
    } 
  }
  
  @Nullable
  static UpdatableSerializable deserialize(Blob blob) {
    Optional<byte[]> optional = blobToByteArray(blob);
    return optional.<UpdatableSerializable>map(UpdatableSerializable::deserialize).orElse(null);
  }
  
  @NotNull
  static Optional<byte[]> blobToByteArray(Blob blob) {
    try {
      byte[] bytes = blob.getBinaryStream().readAllBytes();
      return (Optional)Optional.ofNullable(bytes);
    } catch (IOException|SQLException e) {
      e.printStackTrace();
      return (Optional)Optional.empty();
    } 
  }
  
  int getVersion();
  
  void setVersion(int paramInt);
  
  T getValue();
  
  void setValue(T paramT);
  
  default byte[] serialize() {
    return serialize(this);
  }
  
  default SerialBlob blobSerialize() {
    return blobSerialize(this);
  }
}
