package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey.NONE
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey.INT
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey.LONG
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey.UUID
import com.squareup.kotlinpoet.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

private val json = Json { prettyPrint = true }

class MigrationGenerator(private val petalMigration: PetalMigration) {

    companion object {
        private const val PACKAGE_NAME: String = "com.casadetasha.kexp.petals.migration"
    }

    private lateinit var classBuilder: TypeSpec.Builder

    fun createMigrationForTable() {
        petalMigration.schemaMigrations[1] ?: printThenThrowError("All tables must contain a version 1")
        val className = "TableMigrations\$${petalMigration.tableName}"

        val fileSpecBuilder = FileSpec.builder(
            packageName = PACKAGE_NAME,
            fileName = className
        )

        classBuilder = TypeSpec.classBuilder(className)
        addMigrationSpecs()

        val petalJson = json.encodeToString(petalMigration)
        classBuilder.addStringProperty("petalJson", petalJson)

        fileSpecBuilder.addType(classBuilder.build())
        fileSpecBuilder.build().writeTo(File(AnnotationParser.kaptKotlinGeneratedDir))
    }

    private fun addMigrationSpecs() {
        var previousMigration: PetalSchemaMigration? = null
        petalMigration.schemaMigrations.toSortedMap().forEach {
            val currentMigration = it.value

            currentMigration.migrationSql = when (previousMigration) {
                null -> buildCreateTableSql(currentMigration)
                else -> buildMigrateTableSql(it.key-1 to previousMigration!!,
                    it.key to currentMigration)
            }
            previousMigration = currentMigration
        }
    }

    private fun buildCreateTableSql(petalSchemaMigration: PetalSchemaMigration): String {
        var tableCreationSql = "CREATE TABLE IF NOT EXISTS ${petalMigration.tableName} (" + "\n"

        tableCreationSql += when (petalSchemaMigration.primaryKeyType) {
            NONE -> ""
            INT -> "  id SERIAL AUTO_INCREMENT NOT NULL,\n"
            LONG -> "  id BIGSERIAL AUTO_INCREMENT NOT NULL,\n"
            UUID -> "  id uuid NOT NULL,\n"
        }

        petalSchemaMigration.columnMigrations.values.forEach{
            tableCreationSql += "  ${parseNewColumnSql(it)},\n"
        }
        tableCreationSql = tableCreationSql.removeSuffix(",\n") + "\n"

        if (petalSchemaMigration.primaryKeyType != NONE) {
            tableCreationSql += "  PRIMARY KEY (id)\n"
        }

        tableCreationSql += ")"

        return tableCreationSql
    }

    private fun buildMigrateTableSql(previousMigrationInfo: Pair<Int, PetalSchemaMigration>,
                                     currentMigrationInfo: Pair<Int, PetalSchemaMigration>): String {
        val previousMigration = previousMigrationInfo.second
        val currentMigration = currentMigrationInfo.second
        checkColumnConsistency(previousMigration.columnMigrations, currentMigrationInfo)

        val alteredColumns: Map<String, AlterColumMigration> = getAlteredColumns(previousMigration, currentMigration)
        val addedColumns: List<PetalMigrationColumn> = getAddedColumns(previousMigration, currentMigration)
        val droppedColumns: List<PetalMigrationColumn> =
            getDroppedColumns(previousMigration, currentMigration, alteredColumns)

        var tableMigrationSql = "ALTER TABLE ${petalMigration.tableName}\n"

        tableMigrationSql = tableMigrationSql.amendDroppedColumnSql(droppedColumns)
        tableMigrationSql = tableMigrationSql.amendAlteredColumnSql(alteredColumns)
        tableMigrationSql = tableMigrationSql.amendAddedColumnSql(addedColumns)

        tableMigrationSql = tableMigrationSql.removeSuffix(",\n") + "\n"

        return tableMigrationSql
    }

    private fun checkColumnConsistency(
        previousMigrationColumns: Map<String, PetalMigrationColumn>,
        currentMigrationInfo: Pair<Int, PetalSchemaMigration>
    ) {
        val currentMigrationVersion = currentMigrationInfo.first
        val currentMigration = currentMigrationInfo.second

        currentMigration.columnMigrations.values.forEach {
            val previousColumn = previousMigrationColumns[it.name]
            if(previousColumn != null && !it.isAlteration!! && previousColumn != it) {
                printThenThrowError(
                    "Updated schema for ${it.name} in table ${petalMigration.tableName} version" +
                            " $currentMigrationVersion does not match column from previous schema. If this schema" +
                            " change is intentional, add the @AlterColumn annotation to the column.")
            }
        }

    }

    private fun getDroppedColumns(previousMigration: PetalSchemaMigration,
                                  currentMigration: PetalSchemaMigration,
                                  alteredColumns: Map<String, AlterColumMigration>): List<PetalMigrationColumn> {
        return previousMigration.columnMigrations.values.filter {
            !alteredColumns.containsKey(it.name) && !currentMigration.columnMigrations.containsKey(it.name)
        }
    }

    private fun getAddedColumns(previousMigration: PetalSchemaMigration, currentMigration: PetalSchemaMigration):
            List<PetalMigrationColumn> {
        return currentMigration.columnMigrations.values.filter {
            !it.isAlteration!! && !previousMigration.columnMigrations.containsKey(it.name)
        }
    }

    private fun getAlteredColumns(previousMigration: PetalSchemaMigration, currentMigration: PetalSchemaMigration): Map<String, AlterColumMigration> {
        return currentMigration.columnMigrations.values.filter { it.isAlteration!! }
            .map { AlterColumMigration(previousMigration.columnMigrations[it.previousName]!!, it) }
            .associateBy { it.previousColumnState.name }
    }

}

private fun String.amendAddedColumnSql(addedColumns: List<PetalMigrationColumn>): String {
    var sql = ""
    addedColumns.forEach{ addedColumn ->
        sql += "  ADD COLUMN ${parseNewColumnSql(addedColumn)},\n"
    }

    return this + sql
}

private fun String.amendDroppedColumnSql(droppedColumns: List<PetalMigrationColumn>): String {
    var sql = ""
    droppedColumns.forEach{ droppedColumn ->
        sql += "  DROP COLUMN ${droppedColumn.name},\n"
    }

    return this + sql
}

private fun String.amendAlteredColumnSql(alteredColumns: Map<String, AlterColumMigration>): String {
    var sql = ""
    alteredColumns.values.forEach{ alteredColumn ->
        if (alteredColumn.previousColumnState.name != alteredColumn.updatedColumnState.name) {
            sql += "  RENAME COLUMN ${alteredColumn.previousColumnState.name}" +
                    " TO ${alteredColumn.updatedColumnState.name},\n"
        }
        if (alteredColumn.previousColumnState.isNullable && !alteredColumn.updatedColumnState.isNullable) {
            sql += "  ALTER COLUMN ${alteredColumn.updatedColumnState.name} SET NOT NULL,\n"
        } else if (!alteredColumn.previousColumnState.isNullable && alteredColumn.updatedColumnState.isNullable) {
            sql += "  ALTER COLUMN ${alteredColumn.updatedColumnState.name} DROP NOT NULL,\n"
        }
    }

    return this + sql
}

private fun parseNewColumnSql(column: PetalMigrationColumn): String {

    var sql = "${column.name} ${column.dataType}"
    if (!column.isNullable) {
        sql += " NOT NULL"
    }

    return sql
}

private fun TypeSpec.Builder.addStringProperty(propertyName: String, propertyValue: String) {
    addProperty(PropertySpec.builder(propertyName, String::class)
        .initializer(
            CodeBlock.builder()
                .add("%S", propertyValue)
                .build()
        )
        .build()
    )
}
