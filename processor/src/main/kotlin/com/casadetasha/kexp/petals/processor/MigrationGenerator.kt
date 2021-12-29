package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.squareup.kotlinpoet.*
import java.io.File
import java.util.*

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
                null -> buildCreateTableSpec(currentMigration)
                else -> buildMigrateTableSpec(currentMigration, previousMigration!!)
            }
            classBuilder.addTextSpec("migrateV${it.key}", sqlSpec)
            previousMigration = currentMigration
        }
    }

    private fun buildCreateTableSpec(petalMigration: PetalMigration): String {
        var tableCreationSql = "CREATE TABLE ${petalMigration.tableName} (" + "\n"

        petalMigration.columnMigrations.values.forEach{
            tableCreationSql += "  ${parseNewColumnSql(it)}"
            tableCreationSql += when (it.isNullable) {
                true -> ",\n"
                false -> " NOT NULL,\n"
            }
        }
        tableCreationSql = tableCreationSql.removeSuffix(",\n") + "\n)"

        return tableCreationSql
    }

    private fun buildMigrateTableSpec(currentMigration: PetalMigration, previousMigration: PetalMigration): String {
        val alteredColumns = currentMigration.columnMigrations.values.filter { it.isAlteration }
            .map { AlterColumMigration(previousMigration.columnMigrations[it.previousName]!!, it) }
            .associateBy { it.previousColumnState.name }
        val addedColumns = currentMigration.columnMigrations.values.filter {
            !it.isAlteration && !previousMigration.columnMigrations.containsKey(it.name)
        }
        val droppedColumns = previousMigration.columnMigrations.values.filter {
            !alteredColumns.containsKey(it.name) && !currentMigration.columnMigrations.containsKey(it.name)
        }

        var tableCreationSql = "ALTER TABLE ${currentMigration.tableName}\n"
        alteredColumns.values.forEach{
            if (it.previousColumnState.name != it.updatedColumnState.name) {
                tableCreationSql += "  RENAME COLUMN ${it.previousColumnState.name} TO ${it.updatedColumnState.name},\n"
            }
            if (it.previousColumnState.isNullable && !it.updatedColumnState.isNullable) {
                tableCreationSql += "  ALTER COLUMN ${it.updatedColumnState.name} SET NOT NULL,\n"
            } else if (!it.previousColumnState.isNullable && it.updatedColumnState.isNullable) {
                tableCreationSql += "  ALTER COLUMN ${it.updatedColumnState.name} DROP NOT NULL,\n"
            }
        }
        droppedColumns.forEach{
            tableCreationSql += "  DROP COLUMN ${it.name},\n"
        }
        addedColumns.forEach{
            tableCreationSql += "  ADD COLUMN ${parseNewColumnSql(it)},\n"
        }
        tableCreationSql = tableCreationSql.removeSuffix(",\n") + "\n"

        return tableCreationSql
    }

    private fun parseNewColumnSql(column: PetalMigrationColumn): String {
        val columnSql = when (column.typeName.copy(nullable = false)) {
            String::class.asTypeName() -> "${column.name} TEXT"
            Int::class.asTypeName() -> "${column.name} INT"
            Long::class.asTypeName() -> "${column.name} BIGINT"
            UUID::class.asTypeName() -> "${column.name} UUID"
            else -> printThenThrowError(
                "Type ${column.typeName} was left out of new column sql generation block.")
        }

        return columnSql
    }
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
