package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration

import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.casadetasha.kexp.petals.annotations.PetalMigration
import com.casadetasha.kexp.petals.annotations.PetalSchemaMigration
import com.casadetasha.kexp.petals.processor.model.ParsedPetal
import com.casadetasha.kexp.petals.processor.model.ParsedPetalSchema
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.sqlparser.CreatePetalTableSqlParser
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.sqlparser.MigratePetalTableSqlParser

internal fun parsePetalMigration(petal: ParsedPetal): PetalMigration {
    return PetalMigration(
        tableName = petal.tableName,
        className = petal.baseSimpleName,
        schemaMigrations = parsePetalMigrationSchemas(petal)
    )
}

private fun parsePetalMigrationSchemas(petal: ParsedPetal): Map<Int, PetalSchemaMigration> {
    val migrationSchemas = HashMap<Int, PetalSchemaMigration>()

    var previousSchema: ParsedPetalSchema? = null
    petal.schemas.values.forEach { currentSchema ->
        migrationSchemas[currentSchema.schemaVersion] =
            when(val nullCheckedPreviousSchema = previousSchema) {
                null -> createSchemaForCreateTable(currentSchema)
                else -> createSchemaForTableMigration(nullCheckedPreviousSchema, currentSchema)
            }

        previousSchema = currentSchema
    }

    return migrationSchemas
}

private fun createSchemaForCreateTable(schema: ParsedPetalSchema): PetalSchemaMigration {
    return PetalSchemaMigration(
        primaryKeyType = schema.primaryKeyType,
        columnMap = schema.parsedLocalPetalColumns
            .map {
                PetalColumn(
                    name = it.name,
                    dataType = it.dataType,
                    isNullable = it.isNullable
                )
            }
            .associateBy { it.name },
        preMigrationSql = schema.preMigrationSql,
        migrationSqlRows = CreatePetalTableSqlParser(schema).createTableSql
    )
}

private fun createSchemaForTableMigration(previousSchema: ParsedPetalSchema, currentSchema: ParsedPetalSchema): PetalSchemaMigration {
    val migrationParser = MigratePetalTableSqlParser(previousSchema, currentSchema)

    return PetalSchemaMigration(
        primaryKeyType = currentSchema.primaryKeyType,
        columnMap = currentSchema.parsedLocalPetalColumns
            .map {
                PetalColumn(
                    name = it.name,
                    dataType = it.dataType,
                    isNullable = it.isNullable
                )
            }
            .associateBy { it.name },
        preMigrationSql = currentSchema.preMigrationSql,
        migrationSqlRows = migrationParser.migrateTableSql,
        migrationAlterationSql = migrationParser.renameSql
    )
}
