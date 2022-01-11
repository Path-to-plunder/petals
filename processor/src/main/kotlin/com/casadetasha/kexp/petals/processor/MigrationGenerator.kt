package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.petals.annotations.BasePetalMigration
import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.casadetasha.kexp.petals.annotations.PetalMigration
import com.casadetasha.kexp.petals.annotations.PetalSchemaMigration
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey.*
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey.INT
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey.LONG
import com.squareup.kotlinpoet.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

        classBuilder = TypeSpec.classBuilder(className).superclass(BasePetalMigration::class)
        addMigrationSpecs()

        val petalJson = json.encodeToString(petalMigration)
        classBuilder.addOverriddenStringProperty("petalJson", petalJson)

        fileSpecBuilder.addType(classBuilder.build())
        fileSpecBuilder.build().writeTo(File(AnnotationParser.kaptKotlinGeneratedDir))
    }

    private fun addMigrationSpecs() {
        var previousMigration: PetalSchemaMigration? = null
        petalMigration.schemaMigrations.toSortedMap().forEach {
            val currentMigration = it.value

            currentMigration.migrationSql = when (previousMigration) {
                null -> buildCreateTableSql(currentMigration)
                else -> buildMigrateTableSql(
                    it.key - 1 to previousMigration!!,
                    it.key to currentMigration
                )
            }
            previousMigration = currentMigration
        }
    }

    private fun buildCreateTableSql(petalSchemaMigration: PetalSchemaMigration): String {
        var tableCreationSql = "CREATE TABLE ${petalMigration.tableName} ("

        val primaryKeyType = petalSchemaMigration.primaryKeyType
        tableCreationSql += when (primaryKeyType) {
            NONE -> ""
            INT -> " id ${primaryKeyType.dataType} AUTO_INCREMENT NOT NULL,"
            LONG -> " id ${primaryKeyType.dataType} AUTO_INCREMENT NOT NULL,"
            else -> " id ${primaryKeyType.dataType} NOT NULL,"
        }

        petalSchemaMigration.columnMigrations.values
            .filter { !it.isId!! }
            .forEach {
                tableCreationSql += " ${parseNewColumnSql(it)},"
            }
        tableCreationSql = tableCreationSql.removeSuffix(",")

        if (petalSchemaMigration.primaryKeyType != NONE) {
            tableCreationSql += " PRIMARY KEY (id)"
        }

        tableCreationSql += " )"

        return tableCreationSql
    }

    private fun buildMigrateTableSql(
        previousMigrationInfo: Pair<Int, PetalSchemaMigration>,
        currentMigrationInfo: Pair<Int, PetalSchemaMigration>
    ): String {
        val previousMigration = previousMigrationInfo.second
        val currentMigration = currentMigrationInfo.second
        checkColumnConsistency(previousMigration.columnMigrations, currentMigrationInfo)

        val alteredColumns: Map<String, AlterColumnMigration> = getAlteredColumns(previousMigration, currentMigration)
        val addedColumns: List<PetalColumn> = getAddedColumns(previousMigration, currentMigration)
        val droppedColumns: List<PetalColumn> =
            getDroppedColumns(previousMigration, currentMigration, alteredColumns)

        var tableMigrationSql = "ALTER TABLE ${petalMigration.tableName}"

        tableMigrationSql = tableMigrationSql.amendDroppedColumnSql(droppedColumns)
        tableMigrationSql = tableMigrationSql.amendAlteredColumnSql(alteredColumns)
        tableMigrationSql = tableMigrationSql.amendAddedColumnSql(addedColumns)

        tableMigrationSql = tableMigrationSql.removeSuffix(",")

        return tableMigrationSql
    }

    private fun checkColumnConsistency(
        previousMigrationColumns: Map<String, PetalColumn>,
        currentMigrationInfo: Pair<Int, PetalSchemaMigration>
    ) {
        val currentMigrationVersion = currentMigrationInfo.first
        val currentMigration = currentMigrationInfo.second

        currentMigration.columnMigrations.values
            .filter { !it.isId!! }
            .forEach {
                val previousColumn = previousMigrationColumns[it.name]
                if (previousColumn != null && !it.isAlteration!! && previousColumn != it) {
                    printThenThrowError(
                        "Updated schema for ${it.name} in table ${petalMigration.tableName} version" +
                                " $currentMigrationVersion does not match column from previous schema. If this schema" +
                                " change is intentional, add the @AlterColumn annotation to the column."
                    )
                }
                if (previousColumn != null && it.isAlteration!! && previousColumn.dataType != it.dataType) {
                    printThenThrowError(
                        "Updated schema for ${it.name} in table ${petalMigration.tableName} version" +
                                " $currentMigrationVersion has changed the column data type from" +
                                " ${previousColumn.dataType} to ${it.dataType}. Data type alterations are not" +
                                " currently supported."
                    )
                }
            }
    }

    private fun getDroppedColumns(
        previousMigration: PetalSchemaMigration,
        currentMigration: PetalSchemaMigration,
        alteredColumns: Map<String, AlterColumnMigration>
    ): List<PetalColumn> {
        return previousMigration.columnMigrations.values.filter {
            !alteredColumns.containsKey(it.name) && !currentMigration.columnMigrations.containsKey(it.name)
        }
    }

    private fun getAddedColumns(previousMigration: PetalSchemaMigration, currentMigration: PetalSchemaMigration):
            List<PetalColumn> {
        return currentMigration.columnMigrations.values.filter {
            !it.isAlteration!! && !previousMigration.columnMigrations.containsKey(it.name)
        }
    }

    private fun getAlteredColumns(
        previousMigration: PetalSchemaMigration,
        currentMigration: PetalSchemaMigration
    ): Map<String, AlterColumnMigration> {
        return currentMigration.columnMigrations.values.filter { it.isAlteration!! }
            .map {
                checkNotNull(previousMigration.columnMigrations[it.previousName]) {
                    "Attempting to alter non existent column ${it.previousName} for table ${petalMigration.tableName}"
                }
                AlterColumnMigration(previousMigration.columnMigrations[it.previousName]!!, it)
            }
            .associateBy { it.previousColumnState.name }
    }
}

private fun String.amendAddedColumnSql(addedColumns: List<PetalColumn>): String {
    var sql = ""
    addedColumns.forEach { addedColumn ->
        sql += " ADD COLUMN ${parseNewColumnSql(addedColumn)},"
    }

    return this + sql
}

private fun String.amendDroppedColumnSql(droppedColumns: List<PetalColumn>): String {
    var sql = ""
    droppedColumns.forEach { droppedColumn ->
        sql += " DROP COLUMN ${droppedColumn.name},"
    }

    return this + sql
}

private fun String.amendAlteredColumnSql(alteredColumns: Map<String, AlterColumnMigration>): String {
    var sql = ""
    alteredColumns.values.forEach { alteredColumn ->
        if (alteredColumn.previousColumnState.name != alteredColumn.updatedColumnState.name) {
            sql += " RENAME COLUMN ${alteredColumn.previousColumnState.name}" +
                    " TO ${alteredColumn.updatedColumnState.name},"
        }
        if (alteredColumn.previousColumnState.isNullable && !alteredColumn.updatedColumnState.isNullable) {
            sql += " ALTER COLUMN ${alteredColumn.updatedColumnState.name} SET NOT NULL,"
        } else if (!alteredColumn.previousColumnState.isNullable && alteredColumn.updatedColumnState.isNullable) {
            sql += " ALTER COLUMN ${alteredColumn.updatedColumnState.name} DROP NOT NULL,"
        }
    }

    return this + sql
}

private fun parseNewColumnSql(column: PetalColumn): String {
    var sql = "${column.name} ${column.dataType}"
    if (!column.isNullable) {
        sql += " NOT NULL"
    }

    return sql
}

private fun TypeSpec.Builder.addOverriddenStringProperty(propertyName: String, propertyValue: String) {
    addProperty(
        PropertySpec.builder(propertyName, String::class)
            .initializer(
                CodeBlock.builder()
                    .add("%S", propertyValue)
                    .build()
            ).addModifiers(KModifier.OVERRIDE)
            .build()
    )
}
