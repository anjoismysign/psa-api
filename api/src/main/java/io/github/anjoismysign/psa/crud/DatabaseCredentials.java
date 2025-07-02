package io.github.anjoismysign.psa.crud;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.logging.Logger;

public interface DatabaseCredentials {
  @NotNull
  String getDatabase();
  
  @NotNull
  Identifier getIdentifier();
  
  @Nullable
  Logger getLogger();
  
  @Nullable
  String getHostname();
  
  int getPort();
  
  @Nullable
  String getUsername();
  
  @Nullable
  String getPassword();
  
  @Nullable
  File localDatabaseDirectory();
  
  default boolean isLocalhost() {
    return (getHostname() == null || getUsername() == null || getPassword() == null);
  }
  
  @NotNull
  default File getDirectory() {
    return (localDatabaseDirectory() != null) ? localDatabaseDirectory() : new File(System.getProperty("user.home"));
  }
  
  enum Identifier {
    UUID, PLAYER_NAME;
  }
}
