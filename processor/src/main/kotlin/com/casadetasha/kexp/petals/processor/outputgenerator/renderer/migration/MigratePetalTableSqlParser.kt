package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.petals.processor.inputparser.LocalPetalColumn
import com.casadetasha.kexp.petals.processor.inputparser.ParsedPetalSchema
import com.casadetasha.kexp.petals.processor.inputparser.PetalIdColumn
import com.casadetasha.kexp.petals.processor.model.AlterColumnMigration

internal class MigratePetalTableSqlParser(
    private val previousSchema: ParsedPetalSchema,
    private val currentSchema: ParsedPetalSchema,
): PetalTableSqlParser() {

    private val tableName = currentSchema.tableName

    private val alteredColumns: Map<String, AlterColumnMigration> by lazy {
        currentSchema.parsedLocalPetalColumns.filter { it.isAlteration }
            .map {
                AlterColumnMigration(
                    previousColumnSchema = checkNotNull(previousSchema.parsedLocalPetalColumnMap[it.previousName]) {
                        "Attempting to rename non existent column ${it.previousName} for new column ${it.name} for" +
                                " table ${currentSchema.tableName}"
                    },
                    updatedColumnSchema = it
                )
            }
            .associateBy { it.previousColumnSchema.name }
    }

    private val addedColumns: List<LocalPetalColumn> by lazy {
        currentSchema.parsedLocalPetalColumns.filter { currentColumn ->
            !currentColumn.isAlteration &&
                    !previousSchema.parsedLocalPetalColumns.any { it.name == currentColumn.name }
        }
    }

    private val droppedColumns: List<LocalPetalColumn> by lazy {
        previousSchema.parsedLocalPetalColumns.filter {
            !alteredColumns.containsKey(it.name)
                    && !currentSchema.parsedLocalPetalColumnMap.containsKey(it.name)
                    && it !is PetalIdColumn
        }
    }

    val migrateTableSql: String? by lazy {
        checkColumnConsistency()

        val baseMigrationSql = "ALTER TABLE \"${tableName}\""

        var tableMigrationSql = baseMigrationSql
        tableMigrationSql = tableMigrationSql.amendDroppedColumnSql()
        tableMigrationSql = tableMigrationSql.amendAlteredColumnSql()
        tableMigrationSql = tableMigrationSql.amendAddedColumnSql()

        tableMigrationSql = tableMigrationSql.removeSuffix(",")

        return@lazy when (tableMigrationSql == baseMigrationSql) {
            false -> tableMigrationSql
            true -> null
        }
    }

    val renameSql: List<String> by lazy {
        val renameSqlList = ArrayList<String>()
        alteredColumns.filter { it.value.previousColumnSchema.name != it.value.updatedColumnSchema.name }
            .forEach { (_, columnMigration) ->
                renameSqlList += createRenameColumnSql(columnMigration)
            }
        alteredColumns.filter { it.value.updatedColumnSchema.isRename }
            .forEach { (_, columnMigration) -> checkForNoOpNameChanges(columnMigration) }

        return@lazy renameSqlList
    }

    private fun checkColumnConsistency() {
        currentSchema.parsedLocalPetalColumns
            .filterNot { it is PetalIdColumn }
            .forEach {
                val previousColumn = previousSchema.parsedLocalPetalColumnMap[it.name]
                if (previousColumn != null && !it.isAlteration && previousColumn != it) {
                    printThenThrowError(
                        "Updated schema for ${it.name} in table $tableName version" +
                                " ${currentSchema.schemaVersion} does not match column from previous schema. If this schema" +
                                " change is intentional, add the @AlterColumn annotation to the column."
                    )
                }
                if (previousColumn != null && it.isAlteration && previousColumn.dataType != it.dataType) {
                    printThenThrowError(
                        "Updated schema for ${it.name} in table $tableName version" +
                                " ${currentSchema.schemaVersion} has changed the column data type from" +
                                " ${previousColumn.dataType} to ${it.dataType}. Data type alterations are not" +
                                " currently supported."
                    )
                }
            }
    }

    private fun String.amendAddedColumnSql(): String {
        var sql = ""
        addedColumns.forEach { addedColumn ->
            sql += " ADD COLUMN ${parseNewColumnSql(addedColumn)},"
        }

        return this + sql
    }

    private fun String.amendDroppedColumnSql(): String {
        var sql = ""
        droppedColumns.forEach { droppedColumn ->
            sql += " DROP COLUMN \"${droppedColumn.name}\","
        }

        return this + sql
    }

    private fun String.amendAlteredColumnSql(): String {
        var sql = ""
        alteredColumns.values.forEach { alteredColumn ->
            if (alteredColumn.previousColumnSchema.isNullable && !alteredColumn.updatedColumnSchema.isNullable) {
                sql += " ALTER COLUMN \"${alteredColumn.updatedColumnSchema.name}\" SET NOT NULL,"
            } else if (!alteredColumn.previousColumnSchema.isNullable && alteredColumn.updatedColumnSchema.isNullable) {
                sql += " ALTER COLUMN \"${alteredColumn.updatedColumnSchema.name}\" DROP NOT NULL,"
            }
        }

        return this + sql
    }

    private fun checkForNoOpNameChanges(alteredColumn: AlterColumnMigration) {
        if (alteredColumn.previousColumnSchema.name == alteredColumn.updatedColumnSchema.name) {
            AnnotationParser.printThenThrowError(
                "Attempting to rename column ${alteredColumn.previousColumnSchema.name} to ${alteredColumn.updatedColumnSchema.name} for table" +
                        " ${tableName}, however no name change has occurred. If you are not attempting" +
                        " to change the name, remove the name field from the AlterColumn annotation."
            )
        }
    }

    private fun createRenameColumnSql(alteredColumn: AlterColumnMigration): String {
        return "ALTER TABLE \"${tableName}\" RENAME COLUMN \"${alteredColumn.previousColumnSchema.name}\"" +
                " TO \"${alteredColumn.updatedColumnSchema.name}\";"
    }
}
