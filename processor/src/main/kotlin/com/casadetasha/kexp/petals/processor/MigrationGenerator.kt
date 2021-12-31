package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey.NONE
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey.INT_AUTO_INCREMENT
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey.INT
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey.TEXT
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey.UUID
import com.squareup.kotlinpoet.*
import java.io.File

class MigrationGenerator {

    companion object {
        private const val PACKAGE_NAME: String = "com.casadetasha.kexp.petals.migration"
    }

    private lateinit var classBuilder: TypeSpec.Builder

    fun createMigrationForTable(tableVersionMap: MutableMap<Int, PetalMigration>) {
        val petalMigration: PetalMigration = tableVersionMap[1]
            ?: printThenThrowError("All tables must contain a version 1")
        val className = "TableMigrations\$${petalMigration.tableName}"

        val fileSpecBuilder = FileSpec.builder(
            packageName = PACKAGE_NAME,
            fileName = className
        )

        classBuilder = TypeSpec.classBuilder(className)
        addMigrationSpecs(tableVersionMap)

        fileSpecBuilder.addType(classBuilder.build())
        fileSpecBuilder.build().writeTo(File(AnnotationParser.kaptKotlinGeneratedDir))
    }

    private fun addMigrationSpecs(tableVersionMap: MutableMap<Int, PetalMigration>) {
        var previousMigration: PetalMigration? = null
        tableVersionMap.toSortedMap().forEach {
            val currentMigration = it.value

            val sqlSpec = when (previousMigration) {
                null -> buildCreateTableSql(currentMigration)
                else -> buildMigrateTableSql(previousMigration!!, currentMigration)
            }
            classBuilder.addTextSpec("migrateV${it.key}", sqlSpec)
            previousMigration = currentMigration
        }
    }

    private fun buildCreateTableSql(petalMigration: PetalMigration): String {
        var tableCreationSql = "CREATE TABLE IF NOT EXISTS ${petalMigration.tableName} (" + "\n"

        tableCreationSql += when (petalMigration.primaryKeyType) {
            NONE -> ""
            INT_AUTO_INCREMENT -> "  id INT AUTO_INCREMENT NOT NULL,\n"
            INT -> "  id INT NOT NULL,\n"
            TEXT -> "  id TEXT NOT NULL,\n"
            UUID -> "  id uuid NOT NULL,\n"
        }

        petalMigration.columnMigrations.values.forEach{
            tableCreationSql += "  ${parseNewColumnSql(it)},\n"
        }
        tableCreationSql = tableCreationSql.removeSuffix(",\n") + "\n"

        if (petalMigration.primaryKeyType != NONE) {
            tableCreationSql += "  PRIMARY KEY (id)\n"
        }

        tableCreationSql += ")"

        return tableCreationSql
    }

    private fun buildMigrateTableSql(previousMigration: PetalMigration, currentMigration: PetalMigration): String {
        checkColumnConsistency(previousMigration.columnMigrations, currentMigration)
        val alteredColumns: Map<String, AlterColumMigration> = getAlteredColumns(previousMigration, currentMigration)
        val addedColumns: List<PetalMigrationColumn> = getAddedColumns(previousMigration, currentMigration)
        val droppedColumns: List<PetalMigrationColumn> =
            getDroppedColumns(previousMigration, currentMigration, alteredColumns)

        var tableMigrationSql = "ALTER TABLE ${currentMigration.tableName}\n"

        tableMigrationSql = tableMigrationSql.amendDroppedColumnSql(droppedColumns)
        tableMigrationSql = tableMigrationSql.amendAlteredColumnSql(alteredColumns)
        tableMigrationSql = tableMigrationSql.amendAddedColumnSql(addedColumns)

        tableMigrationSql = tableMigrationSql.removeSuffix(",\n") + "\n"

        return tableMigrationSql
    }

    private fun checkColumnConsistency(previousMigrationColumns: Map<String, PetalMigrationColumn>,
                                       currentMigration: PetalMigration) {
        currentMigration.columnMigrations.values.forEach {
            val previousColumn = previousMigrationColumns[it.name]
            if(previousColumn != null && !it.isAlteration && previousColumn != it) {
                printThenThrowError(
                    "Updated schema for ${it.name} in table ${currentMigration.tableName} version" +
                            " ${currentMigration.version} does not match column from previous schema. If this schema" +
                            " change is intentional, add the @AlterColumn annotation to the column.")
            }
        }

    }

    private fun getDroppedColumns(previousMigration: PetalMigration,
                                  currentMigration: PetalMigration,
                                  alteredColumns: Map<String, AlterColumMigration>): List<PetalMigrationColumn> {
        return previousMigration.columnMigrations.values.filter {
            !alteredColumns.containsKey(it.name) && !currentMigration.columnMigrations.containsKey(it.name)
        }
    }

    private fun getAddedColumns(previousMigration: PetalMigration, currentMigration: PetalMigration):
            List<PetalMigrationColumn> {
        return currentMigration.columnMigrations.values.filter {
            !it.isAlteration && !previousMigration.columnMigrations.containsKey(it.name)
        }
    }

    private fun getAlteredColumns(previousMigration: PetalMigration, currentMigration: PetalMigration): Map<String, AlterColumMigration> {
        return currentMigration.columnMigrations.values.filter { it.isAlteration }
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

private fun TypeSpec.Builder.addTextSpec(methodName: String, migrationSql: String) {
    addFunction(FunSpec.builder(methodName)
        .returns(String::class)
        .addCode(
            CodeBlock.builder()
                .add("return %S", migrationSql)
                .build()
        )
        .build())
}
