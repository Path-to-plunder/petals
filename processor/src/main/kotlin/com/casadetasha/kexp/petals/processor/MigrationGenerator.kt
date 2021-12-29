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
        var tableCreationSql = "CREATE TABLE ${petalMigration.tableName} ( "

        petalMigration.columnMigrations.forEach{
            tableCreationSql += "${parseNewColumnSql(it)}, "
        }
        tableCreationSql = tableCreationSql.removeSuffix(", ")
        tableCreationSql += " )"

        return tableCreationSql
    }

    private fun buildMigrateTableSpec(currentMigration: PetalMigration, previousMigration: PetalMigration): String {
        val alteredColumns = currentMigration.columnMigrations.filter { it.isAlteration }
        val addedColumns = currentMigration.columnMigrations.filter {
            !it.isAlteration && !previousMigration.columnMigrations.contains(it)
        }
        val droppedColumns = previousMigration.columnMigrations.filter {
            if (currentMigration.columnMigrations.contains(it)) return@filter false
            return@filter !alteredColumns.map { column -> column.previousName }.contains(it.name)
        }

        var tableCreationSql = "ALTER TABLE ${currentMigration.tableName}\n"
        alteredColumns.forEach{
            tableCreationSql += "  RENAME COLUMN ${it.previousName} TO ${it.name},\n"
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
        val columnSql = when (column.typeName) {
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
