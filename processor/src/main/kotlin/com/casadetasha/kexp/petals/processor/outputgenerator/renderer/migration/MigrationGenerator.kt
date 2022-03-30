package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.petals.annotations.BasePetalMigration
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey.INT
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey.LONG
import com.casadetasha.kexp.petals.processor.inputparser.LocalPetalColumn
import com.casadetasha.kexp.petals.processor.inputparser.ParsedPetal
import com.casadetasha.kexp.petals.processor.inputparser.ParsedPetalSchema
import com.casadetasha.kexp.petals.processor.inputparser.PetalIdColumn
import com.casadetasha.kexp.petals.processor.model.AlterColumnMigration
import com.squareup.kotlinpoet.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.collections.ArrayList

private val json = Json { prettyPrint = true }

internal class MigrationGenerator(private val petalMigration: ParsedPetal) {

    companion object {
        private const val PACKAGE_NAME: String = "com.casadetasha.kexp.petals.migration"
    }

    private lateinit var classBuilder: TypeSpec.Builder

    fun createMigrationForTable() {
        petalMigration.schemas[1] ?: printThenThrowError("All tables must contain a version 1")
        val className = "TableMigrations\$${petalMigration.tableName}"

        val fileSpecBuilder = FileSpec.builder(
            packageName = PACKAGE_NAME,
            fileName = className
        )

        classBuilder = TypeSpec.classBuilder(className).superclass(BasePetalMigration::class)
        addMigrationSpecs()

        val petalJson = json.encodeToString(petalMigration.processMigration())
        classBuilder.addOverriddenStringProperty("petalJson", petalJson)

        fileSpecBuilder.addType(classBuilder.build())
        fileSpecBuilder.build().writeTo(File(AnnotationParser.kaptKotlinGeneratedDir))
    }

    private fun addMigrationSpecs() {
        var previousSchema: ParsedPetalSchema? = null
        petalMigration.schemas.forEach { (_, currentSchema) ->

            currentSchema.migrationSql = when (val nullCheckedPreviousSchema = previousSchema) {
                null -> buildCreateTableSql(currentSchema)
                else -> buildMigrateTableSql(
                    nullCheckedPreviousSchema,
                    currentSchema
                )
            }
            if (previousSchema != null) {
                currentSchema.migrationAlterationSql = createRenameSql(previousSchema!!, currentSchema)
            }
            previousSchema = currentSchema
        }
    }

    private fun createRenameSql(
        previousSchema: ParsedPetalSchema,
        currentSchema: ParsedPetalSchema
    ): List<String> {
        val renameSqlList = ArrayList<String>()
        val alteredColumns: Map<String, AlterColumnMigration> = getAlteredColumns(previousSchema, currentSchema)
        alteredColumns.filter { it.value.previousColumnSchema.name != it.value.updatedColumnSchema.name }
            .forEach { (_, migration) ->
                renameSqlList += createRenameColumnSql(migration)
            }
        alteredColumns.filter { it.value.updatedColumnSchema.isRename }
            .forEach { (_, migration) -> checkForNoOpNameChanges(migration) }

        return renameSqlList
    }

    private fun checkForNoOpNameChanges(alteredColumn: AlterColumnMigration) {
        if (alteredColumn.previousColumnSchema.name == alteredColumn.updatedColumnSchema.name) {
            printThenThrowError(
                "Attempting to rename column ${alteredColumn.previousColumnSchema.name} to ${alteredColumn.updatedColumnSchema.name} for table" +
                        " ${petalMigration.tableName}, however no name change has occurred. If you are not attempting" +
                        " to change the name, remove the name field from the AlterColumn annotation."
            )
        }
    }

    private fun buildCreateTableSql(petalSchema: ParsedPetalSchema): String {
        var tableCreationSql = "CREATE TABLE \"${petalMigration.tableName}\" ("

        val primaryKeyType = petalSchema.primaryKeyType
        tableCreationSql += when (primaryKeyType) {
            INT -> " id ${primaryKeyType.dataType} PRIMARY KEY,"
            LONG -> " id ${primaryKeyType.dataType} PRIMARY KEY,"
            else -> " id ${primaryKeyType.dataType} PRIMARY KEY,"
        }

        petalSchema.parsedLocalPetalColumns
            .filterNot { it is PetalIdColumn }
            .forEach {
                tableCreationSql += " ${parseNewColumnSql(it)},"
            }
        tableCreationSql = tableCreationSql.removeSuffix(",")

        tableCreationSql += " )"

        return tableCreationSql
    }

    private fun buildMigrateTableSql(
        previousSchema: ParsedPetalSchema,
        currentSchema: ParsedPetalSchema
    ): String? {
        checkColumnConsistency(previousSchema, currentSchema)

        val alteredColumns: Map<String, AlterColumnMigration> = getAlteredColumns(previousSchema, currentSchema)
        val addedColumns: List<LocalPetalColumn> = getAddedColumns(previousSchema, currentSchema)
        val droppedColumns: List<LocalPetalColumn> =
            getDroppedColumns(previousSchema, currentSchema, alteredColumns)

        val baseMigrationSql = "ALTER TABLE \"${petalMigration.tableName}\""

        var tableMigrationSql = baseMigrationSql
        tableMigrationSql = tableMigrationSql.amendDroppedColumnSql(droppedColumns)
        tableMigrationSql = tableMigrationSql.amendAlteredColumnSql(alteredColumns)
        tableMigrationSql = tableMigrationSql.amendAddedColumnSql(addedColumns)

        tableMigrationSql = tableMigrationSql.removeSuffix(",")

        return when (tableMigrationSql == baseMigrationSql) {
            false -> tableMigrationSql
            true -> null
        }
    }

    private fun checkColumnConsistency(
        previousColumns: ParsedPetalSchema,
        currentSchema: ParsedPetalSchema
    ) {
        currentSchema.parsedLocalPetalColumns
            .filterNot { it is PetalIdColumn }
            .forEach {
                val previousColumn = previousColumns.parsedLocalPetalColumnMap[it.name]
                if (previousColumn != null && !it.isAlteration && previousColumn != it) {
                    printThenThrowError(
                        "Updated schema for ${it.name} in table ${petalMigration.tableName} version" +
                                " ${currentSchema.schemaVersion} does not match column from previous schema. If this schema" +
                                " change is intentional, add the @AlterColumn annotation to the column."
                    )
                }
                if (previousColumn != null && it.isAlteration && previousColumn.dataType != it.dataType) {
                    printThenThrowError(
                        "Updated schema for ${it.name} in table ${petalMigration.tableName} version" +
                                " ${currentSchema.schemaVersion} has changed the column data type from" +
                                " ${previousColumn.dataType} to ${it.dataType}. Data type alterations are not" +
                                " currently supported."
                    )
                }
            }
    }

    private fun getDroppedColumns(
        previousMigration: ParsedPetalSchema,
        currentMigration: ParsedPetalSchema,
        alteredColumns: Map<String, AlterColumnMigration>
    ): List<LocalPetalColumn> {
        return previousMigration.parsedLocalPetalColumns.filter {
            !alteredColumns.containsKey(it.name)
                    && !currentMigration.parsedLocalPetalColumnMap.containsKey(it.name)
                    && !( it is PetalIdColumn )
        }
    }

    private fun getAddedColumns(
        previousSchema: ParsedPetalSchema, currentSchema: ParsedPetalSchema): List<LocalPetalColumn> {
        return currentSchema.parsedLocalPetalColumns.filter { currentColumn ->
            !currentColumn.isAlteration &&
                    !previousSchema.parsedLocalPetalColumns.any { it.name == currentColumn.name }
        }
    }

    private fun getAlteredColumns(
        previousSchema: ParsedPetalSchema,
        currentSchema: ParsedPetalSchema
    ): Map<String, AlterColumnMigration> {
        return currentSchema.parsedLocalPetalColumns.filter { it.isAlteration }
            .map {
                AlterColumnMigration(
                     previousColumnSchema = checkNotNull(previousSchema.parsedLocalPetalColumnMap[it.previousName]) {
                        "Attempting to rename non existent column ${it.previousName} for new column ${it.name} for" +
                                " table ${petalMigration.tableName}"
                    },
                    updatedColumnSchema = it
                )
            }
            .associateBy { it.previousColumnSchema.name }
    }


    private fun createRenameColumnSql(alteredColumn: AlterColumnMigration): String {
        return "ALTER TABLE \"${petalMigration.tableName}\" RENAME COLUMN \"${alteredColumn.previousColumnSchema.name}\"" +
                " TO \"${alteredColumn.updatedColumnSchema.name}\";"
    }
}

private fun String.amendAddedColumnSql(addedColumns: List<LocalPetalColumn>): String {
    var sql = ""
    addedColumns.forEach { addedColumn ->
        sql += " ADD COLUMN ${parseNewColumnSql(addedColumn)},"
    }

    return this + sql
}

private fun String.amendDroppedColumnSql(droppedColumns: List<LocalPetalColumn>): String {
    var sql = ""
    droppedColumns.forEach { droppedColumn ->
        sql += " DROP COLUMN \"${droppedColumn.name}\","
    }

    return this + sql
}

private fun String.amendAlteredColumnSql(alteredColumns: Map<String, AlterColumnMigration>): String {
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

private fun parseNewColumnSql(column: LocalPetalColumn): String {
    var sql = "\"${column.name}\" ${column.dataType}"
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
