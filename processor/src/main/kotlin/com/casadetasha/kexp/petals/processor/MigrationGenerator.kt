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

        tableVersionMap.toSortedMap().forEach {
            classBuilder.addTextSpec("migrateV${it.key}", buildClassSpec(it.value))
        }

        fileSpecBuilder.addType(classBuilder.build())
        fileSpecBuilder.build().writeTo(File(AnnotationParser.kaptKotlinGeneratedDir))
    }

    private fun buildClassSpec(petalMigration: PetalMigration): String {
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
