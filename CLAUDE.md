# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Full build
mvn clean install

# Build specific module
mvn install -pl api      # Build api module only
mvn install -pl sql      # Build sql module only

# Run a specific test (if tests exist)
mvn test -Dtest=ClassName
```

This is a Maven multi-module project requiring Java 17.

## Architecture

### Module Structure

- **`api`** - Core interfaces and abstractions (depends only on `aesthetic` library)
- **`sql`** - Database implementations: MySQL and SQLite support using HikariCP

### Core Interfaces

**`Crudable`** (`api/src/main/java/io/github/anjoismysign/psa/crud/Crudable.java`)
- Base marker interface for entities that can be persisted to database
- Implementations must provide `getIdentification()` returning a unique String

**`CrudDatabase<T>`** (`api/.../crud/CrudDatabase.java`)
- Factory interface for creating `CrudManager<T>` instances
- Implementations like `SQLiteCrudDatabase` and `MySQLCrudDatabase` handle specific database logic

**`CrudManager<T>`** (`api/.../crud/CrudManager.java`)
- Main CRUD interface: `create()`, `read()`, `update()`, `delete()`, `exists()`
- Handles serialization/deserialization via `UpdatableSerializable<T>`
- Supports lifecycle hooks via `PostLoadable` and `PreUpdatable`

**`UpdatableSerializable<T>`** (`api/.../UpdatableSerializable.java`)
- Versioned, serializable wrapper for CRUD entities
- Stores `version` field and original `value` of type T
- Provides static `serialize()`/`deserialize()` methods using Java serialization
- Objects implementing this interface are what gets stored in the database

**`SerializableManager<T>`** (`api/.../serializablemanager/SerializableManager.java`)
- Manages in-memory cache of deserialized entities
- Handles `deserialize()`, `cacheLook()`, `save()`, `syncSaveAll()`

### Lifecycle Hooks

- **`PostLoadable.onPostLoad()`** - Invoked after `CrudManager.read()` loads an entity, runs in the same thread as the read operation
- **`PreUpdatable.onPreUpdate()`** - Invoked before `CrudManager.update()` saves changes

### Connection Pooling

**`SQLDatabaseRegistry`** (`sql/.../SQLDatabaseRegistry.java`) manages shared HikariCP connection pools:
- `acquire(key, factory)` - Gets or creates a database instance, incrementing reference count
- `release(key)` - Decrements reference count, disconnects when count reaches zero
- Key formats: `file path` for SQLite, `user@host:port/database` for MySQL

**`SQLDatabase`** is the abstract base for MySQL and SQLite implementations with:
- HikariCP connection pooling (200 max connections, 5 min idle, 15s leak detection)
- Common query methods: `selectRowByPrimaryKey`, `selectAllFromDatabase`, `createTable`, `exists`

### Key Implementation Classes

- `SQLiteCrudManager<T>` / `MySQLCrudManager<T>` - Concrete CRUD operations
- `SQLiteCrudDatabase<T>` / `MySQLCrudDatabase<T>` - Database setup and container generation
- `Lehmapp` - A `Map<String, Object>` implementation used as a flexible serializable container