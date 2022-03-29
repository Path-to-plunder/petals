package com.casadetasha.kexp.petals.processor.migration

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.petals.annotations.BasePetalMigration
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey.INT
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey.LONG
import com.casadetasha.kexp.petals.processor.UnprocessedPetalColumn
import com.casadetasha.kexp.petals.processor.UnprocessedPetalMigration
import com.casadetasha.kexp.petals.processor.UnprocessedPetalSchemaMigration
import com.squareup.kotlinpoet.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private val json = Json { prettyPrint = true }

internal class MigrationGenerator(private val petalMigration: UnprocessedPetalMigration) {

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

        val petalJson = json.encodeToString(petalMigration.process())
        classBuilder.addOverriddenStringProperty("petalJson", petalJson)

        fileSpecBuilder.addType(classBuilder.build())
        fileSpecBuilder.build().writeTo(File(AnnotationParser.kaptKotlinGeneratedDir))
    }

    private fun addMigrationSpecs() {
        var previousMigration: UnprocessedPetalSchemaMigration? = null
        petalMigration.schemaMigrations.toSortedMap().forEach {
            val currentMigration = it.value

            currentMigration.migrationSql = when (previousMigration) {
                null -> buildCreateTableSql(currentMigration)
                else -> buildMigrateTableSql(
                    it.key - 1 to previousMigration!!,
                    it.key to currentMigration
                )
            }
            if (previousMigration != null) {
                currentMigration.migrationAlterationSql = createRenameSql(previousMigration!!, currentMigration)
            }
            previousMigration = currentMigration
        }
    }

    private fun createRenameSql(
        previousMigration: UnprocessedPetalSchemaMigration,
        currentMigration: UnprocessedPetalSchemaMigration
    ): List<String> {
        val renameSqlList = ArrayList<String>()
        val alteredColumns: Map<String, AlterColumnMigration> = getAlteredColumns(previousMigration, currentMigration)
        alteredColumns.filter { it.value.previousColumnState.name != it.value.updatedColumnState.name }
            .forEach { (_, migration) ->
                renameSqlList += createRenameColumnSql(migration)
            }
        alteredColumns.filter { it.value.updatedColumnState.isRename }
            .forEach { (_, migration) -> checkForNoOpNameChanges(migration) }

        return renameSqlList
    }

    private fun checkForNoOpNameChanges(alteredColumn: AlterColumnMigration) {
        if (alteredColumn.previousColumnState.name == alteredColumn.updatedColumnState.name) {
            printThenThrowError(
                "Attempting to rename column ${alteredColumn.previousColumnState.name} to ${alteredColumn.updatedColumnState.name} for table" +
                        " ${petalMigration.tableName}, however no name change has occurred. If you are not attempting" +
                        " to change the name, remove the name field from the AlterColumn annotation."
            )
        }
    }

    private fun buildCreateTableSql(petalSchemaMigration: UnprocessedPetalSchemaMigration): String {
        var tableCreationSql = "CREATE TABLE \"${petalMigration.tableName}\" ("

        val primaryKeyType = petalSchemaMigration.primaryKeyType
        tableCreationSql += when (primaryKeyType) {
            INT -> " id ${primaryKeyType.dataType} PRIMARY KEY,"
            LONG -> " id ${primaryKeyType.dataType} PRIMARY KEY,"
            else -> " id ${primaryKeyType.dataType} PRIMARY KEY,"
        }

        petalSchemaMigration.localColumnMigrations.values
            .filter { !it.isId }
            .forEach {
                tableCreationSql += " ${parseNewColumnSql(it)},"
            }
        tableCreationSql = tableCreationSql.removeSuffix(",")

        tableCreationSql += " )"

        return tableCreationSql
    }

    private fun buildMigrateTableSql(
        previousMigrationInfo: Pair<Int, UnprocessedPetalSchemaMigration>,
        currentMigrationInfo: Pair<Int, UnprocessedPetalSchemaMigration>
    ): String? {
        val previousMigration = previousMigrationInfo.second
        val currentMigration = currentMigrationInfo.second
        checkColumnConsistency(previousMigration.localColumnMigrations, currentMigrationInfo)

        val alteredColumns: Map<String, AlterColumnMigration> = getAlteredColumns(previousMigration, currentMigration)
        val addedColumns: List<UnprocessedPetalColumn> = getAddedColumns(previousMigration, currentMigration)
        val droppedColumns: List<UnprocessedPetalColumn> =
            getDroppedColumns(previousMigration, currentMigration, alteredColumns)

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
        previousMigrationColumns: Map<String, UnprocessedPetalColumn>,
        currentMigrationInfo: Pair<Int, UnprocessedPetalSchemaMigration>
    ) {
        val currentMigrationVersion = currentMigrationInfo.first
        val currentMigration = currentMigrationInfo.second

        currentMigration.localColumnMigrations.values
            .filter { !it.isId }
            .forEach {
                val previousColumn = previousMigrationColumns[it.name]
                if (previousColumn != null && !it.isAlteration && previousColumn.petalColumn != it.petalColumn) {
                    printThenThrowError(
                        "Updated schema for ${it.name} in table ${petalMigration.tableName} version" +
                                " $currentMigrationVersion does not match column from previous schema. If this schema" +
                                " change is intentional, add the @AlterColumn annotation to the column."
                    )
                }
                if (previousColumn != null && it.isAlteration && previousColumn.dataType != it.dataType) {
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
        previousMigration: UnprocessedPetalSchemaMigration,
        currentMigration: UnprocessedPetalSchemaMigration,
        alteredColumns: Map<String, AlterColumnMigration>
    ): List<UnprocessedPetalColumn> {
        return previousMigration.localColumnMigrations.values.filter {
            !alteredColumns.containsKey(it.name)
                    && !currentMigration.localColumnMigrations.containsKey(it.name)
                    && !it.isId
        }
    }

    private fun getAddedColumns(
        previousMigration: UnprocessedPetalSchemaMigration,
        currentMigration: UnprocessedPetalSchemaMigration
    ):
            List<UnprocessedPetalColumn> {
        return currentMigration.localColumnMigrations.values.filter {
            !it.isAlteration && !previousMigration.localColumnMigrations.containsKey(it.name)
        }
    }

    private fun getAlteredColumns(
        previousMigration: UnprocessedPetalSchemaMigration,
        currentMigration: UnprocessedPetalSchemaMigration
    ): Map<String, AlterColumnMigration> {
        return currentMigration.localColumnMigrations.values.filter { it.isAlteration }
            .map {
                val previousColumnMigrations = checkNotNull(previousMigration.localColumnMigrations[it.previousName]) {
                    "Attempting to rename non existent column ${it.previousName} for new column ${it.name} for table" +
                            " ${petalMigration.tableName}"
                }
                AlterColumnMigration(previousColumnMigrations, it)
            }
            .associateBy { it.previousColumnState.name }
    }


    private fun createRenameColumnSql(alteredColumn: AlterColumnMigration): String {
        return "ALTER TABLE \"${petalMigration.tableName}\" RENAME COLUMN \"${alteredColumn.previousColumnState.name}\"" +
                " TO \"${alteredColumn.updatedColumnState.name}\";"
    }
}

private fun String.amendAddedColumnSql(addedColumns: List<UnprocessedPetalColumn>): String {
    var sql = ""
    addedColumns.forEach { addedColumn ->
        sql += " ADD COLUMN ${parseNewColumnSql(addedColumn)},"
    }

    return this + sql
}

private fun String.amendDroppedColumnSql(droppedColumns: List<UnprocessedPetalColumn>): String {
    var sql = ""
    droppedColumns.forEach { droppedColumn ->
        sql += " DROP COLUMN \"${droppedColumn.name}\","
    }

    return this + sql
}

private fun String.amendAlteredColumnSql(alteredColumns: Map<String, AlterColumnMigration>): String {
    var sql = ""
    alteredColumns.values.forEach { alteredColumn ->
        if (alteredColumn.previousColumnState.isNullable && !alteredColumn.updatedColumnState.isNullable) {
            sql += " ALTER COLUMN \"${alteredColumn.updatedColumnState.name}\" SET NOT NULL,"
        } else if (!alteredColumn.previousColumnState.isNullable && alteredColumn.updatedColumnState.isNullable) {
            sql += " ALTER COLUMN \"${alteredColumn.updatedColumnState.name}\" DROP NOT NULL,"
        }
    }

    return this + sql
}

private fun parseNewColumnSql(column: UnprocessedPetalColumn): String {
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
