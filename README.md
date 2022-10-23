# Petals

## Overview

Petals are a way to manage database interactions without boilerplate.

This means:
* No creating multiple classes or interfaces with the same values for exposed
* No manual creation of accessor or serializable data classes
* No manual database migrations
* No manual transaction management (still available if your use case needs it)

Petals manage this in 2 ways:

1) Database precheck/migrations
    * Checks your current database to see if the table already exists
    * Creates a petal meta table to manage versions (if table match is found, it is assumed to be schema version 1)
    * Compares schema values to validate data before migrating
    * Runs through all migrations in order starting with the current database version in the meta table (or creates a new table for schema version 1 if no existing table is found)
   
2) Generated database classes
    * Exposed Table/Entity classes
    * Data accessor class to manage transactions under the hood
    * Kotlin Data class, along with extension methods for the entity and accessor classes to export to it

In its current alpha state, it supports Postgres databases using Hikari as your database connector.

## Getting started

### Setup
Inside your application's setup: call Exposed's `Database.connect(dbSource)` method, then call the generated `setupAndMigrateTables(dbSource)` method with the same `dbSource`.

```
fun setupDb() {
    val dbSource = createHikariDataSource()
    Database.connect(dbSource)
    PetalTables.setupAndMigrateTables(dbSource)
}

fun createHikariDataSource(): HikariDataSource = // configure your own hikari connection here
```

###  Creating a table
To create a Petal, you will need to define both the `Petal` and the `PetalSchema`.

* The Petal contains the table name, primary ID type, and generated accessor class name.

* The PetalSchema contains a reference to the Petal, version number, and all column data.

```kt
ExamplePetalSchema.kt

@Petal(tableName = "example_table", className = "ExampleClass", primaryKeyType = PetalPrimaryKey.INT)
interface ExamplePetal

@PetalSchema(petal = ExamplePetal::class)
interface ExamplePetalSchema {
    val name: String
}
```

### Accessing your data

#### Creating
```kt
MyServer.kt

fun create(name: String): ExampleClass {
    return ExampleClass.create(name = name)
}
```

#### Loading
```kt
MyServer.kt

fun load(id: Int): ExampleClass {
    return ExampleClass.load(id)
}
```

#### Updating
```kt
MyServer.kt

val myExample: ExampleClass // already loaded ExampleClass

fun update(newName: String) {
    myExample.name = newName
    ExampleClass.store(myExample)
}
```

#### Deleting
```kt
MyServer.kt

val myExample: ExampleClass // already loaded ExampleClass

fun update(newName: String) {
    ExampleClass.delete(myExample)
}
```

#### Querying
```kt
MyServer.kt

fun query(name: String): List<ExampleClass> {
    return ExampleClass.loadWithQuery { table -> table.name eq name }
}
```

### Serializing

```kt
MyServer.kt

fun loadAsJsonString(id: Int): String {
    val loadedExample = ExampleClass.load(id)
    return Json.encodeToString( loadedExample.asData() )
}
```

## Full API

### Default annotation values

#### @Petal ID
Petals with no primary key type specified will default to UUID

#### @PetalSchema version
Petals with no version specified will default to version 1

### Supported basic columns
Here is a sample PetalSchema with all supported column types

```kt
@PetalSchema(petal = BasicPetal::class)
interface BasicPetalSchema {
    val uuidColumn: UUID
    val intColumn: Int
    val longColumn: Long
    val stringColumn: String
    @VarChar(charLimit = 10) val varCharColumn: String
}
```

### Optional columns
All column types recognize Kotlin's null declaration. Simply add `?` to the column type. This is true for Nested Petals as well.

```kt
@PetalSchema(petal = BasicPetal::class)
interface BasicPetalSchema {
    val uuidColumn: UUID?
    val intColumn: Int?
    val longColumn: Long?
    val stringColumn: String?
    @VarChar(charLimit = 10) val varCharColumn: String?
}
```

### Nested Petals
To nest a Petal, create a column with the type of the nested Petal class (the class annotated with @Petal)

#### Setup

```kt
@Petal(tableName = "parent_petal", className = "ParentPetalClass")
interface ParentPetal

@Petal(tableName = "nested_petal", className = "NestedPetalClass")
interface NestedPetal

@PetalSchema(petal = ParentPetal::class)
interface ParentPetalSchema {
    val name: String
    val nestedPetal: NestedPetal
}

@PetalSchema(petal = NestedPetal::class)
interface NestedPetalSchema {
    val name: String
}
```

#### Usage

```kt
val nestedPetal = NestedPetalClass.create(name = "hola nestie")

val parentPetal: ParentPetalClass = ParentPetalClass.create(
    name = "My name",
    nestedPetal = nestedPetal
)

val loadedNestedPetal = ParentPetalClass.load(parentPetal.id).nestedPetal
```

### Referencing Parent Petals
To add a reference to a Parent table, create a column with the type of the parent Petal class (the class annotated with @Petal). Annotate the column with @ReferencedBy({name of column in parent petal})

#### Setup

```kt
@Petal(tableName = "parent_petal", className = "ParentPetalClass")
interface ParentPetal

@Petal(tableName = "nested_petal", className = "NestedPetalClass")
interface NestedPetal

@PetalSchema(petal = ParentPetal::class)
interface ParentPetalSchema {
    val name: String
    val nestedPetal: NestedPetal
}

@PetalSchema(petal = NestedPetal::class)
interface NestedPetalSchema {
    val name: String
    @ReferencedBy("nestedPetal") val parents: ParentPetal
}
```

#### Usage

```kt
val nestedPetal = NestedPetalClass.create(name = "hola nestie")

val parentPetal: ParentPetalClass = ParentPetalClass.create(
    name = "My name",
    nestedPetal = nestedPetal
)

val loadedParentPetal = ParentPetalClass.load(nestedPetal.id).parents.first()
```

### Migrations

#### Adding/Removing Columns
Adding and removing columns is managed by creating a new Schema with an incremented version number.

```kt

@PetalSchema(petal = MigratedPetal::class, version = 1)
interface MigratedPetalSchemaV1 {
    val uuid: UUID
}

// Add a column
@PetalSchema(petal = MigratedPetal::class, version = 2)
interface MigratedPetalSchemaV2 {
    val uuid: UUID
    val color: String
}

// Remove a column
@PetalSchema(petal = MigratedPetal::class, version = 3)
interface MigratedPetalSchemaV3 {
    val color: String
}

// Add and remove a column
@PetalSchema(petal = MigratedPetal::class, version = 4)
interface MigratedPetalSchemaV4 {
    val uuid: UUID
}
```

#### Renaming columns
Create a new migration with the new column name, and annotate it with `@AlterColumn` with a `renameFrom` value matching the previous column name.

```kt
@PetalSchema(petal = MigratedPetal::class, version = 1)
interface MigratedPetalSchemaV1 {
    val uuid: UUID
}

@PetalSchema(petal = MigratedPetal::class, version = 2)
abstract class MigratedPetalSchemaV3 {
    @AlterColumn(renameFrom = "uuid") val renamed_uuid: UUID
}
```

### Default column values
Use the matching @DefaultType() annotation on the column

```kt
@PetalSchema(petal = DefaultValuePetal::class)
interface DefaultValuePetalSchema {
    @DefaultString("default string value") val stringValue: String
    @DefaultInt(10) val intValue: Int
    @DefaultLong(200) val longValue: Long
}
```

### Pre-migration sql
To run custom sql before a migration, add a `@ExecuteSqlBeforeMigration("SQL GOES HERE")` annotation to the migration you want to run it before.

Post-migration sql is planned to be supported, but has not yet been implemented.

### Accessing Exposed DB Entity
The Exposed DB entity is a field on every accessor called dbEntity

#### sample
```kt
MyServer.kt

fun loadEntity(id: Int): ExampleClassEntity {
    return ExampleClass.load(id).dbEntity
}
```
