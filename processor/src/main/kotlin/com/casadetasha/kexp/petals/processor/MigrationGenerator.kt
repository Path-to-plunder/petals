package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.squareup.kotlinpoet.*
import java.io.File

class MigrationGenerator {

    companion object {
        private const val PACKAGE_NAME: String = "com.casadetasha.kexp.petals.migration"
    }

    private lateinit var classBuilder: TypeSpec.Builder

    fun createMigrationForTable(tableVersionMap: MutableMap<Int, PetalMigration>) {
        val petalMigration: PetalMigration = tableVersionMap[1]
            ?: printThenThrowError("All tables must start with version 1")
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

        petalMigration.columns.forEach{
            tableCreationSql += when (it.typeName) {
                String::class.asTypeName() -> "${it.name} TEXT, "
                Int::class.asTypeName() -> "${it.name} INT, "
                else -> printThenThrowError("Only String and Int types are currently supported.")
            }
        }
        tableCreationSql = tableCreationSql.removeSuffix(", ")
        tableCreationSql += " )"
        return tableCreationSql
    }

    private fun buildMigrateTableSpec(currentMigration: PetalMigration, previousMigration: PetalMigration): String {
        val addedColumns = currentMigration.columns.filter { !previousMigration.columns.contains(it) }
        val droppedColumns = previousMigration.columns.filter { !currentMigration.columns.contains(it) }
        var tableCreationSql = "MIGRATE TABLE ${currentMigration.tableName} ( "

        addedColumns.forEach{
            tableCreationSql += when (it.typeName) {
                String::class.asTypeName() -> "ADD COLUMN ${it.name} TEXT, "
                Int::class.asTypeName() -> "ADD COLUMN ${it.name} INT, "
                else -> printThenThrowError("Only String and Int types are currently supported.")
            }
        }

        droppedColumns.forEach{
            tableCreationSql += when (it.typeName) {
                String::class.asTypeName() -> "DROP COLUMN ${it.name}, "
                Int::class.asTypeName() -> "DROP COLUMN ${it.name}, "
                else -> printThenThrowError("Only String and Int types are currently supported.")
            }
        }

        tableCreationSql = tableCreationSql.removeSuffix(", ")
        tableCreationSql += " )"
        return tableCreationSql
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
