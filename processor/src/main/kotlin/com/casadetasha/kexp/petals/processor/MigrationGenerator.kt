package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.annotationparser.KotlinContainer
import com.casadetasha.kexp.petals.annotations.Petal
import com.squareup.kotlinpoet.*
import java.io.File

class MigrationGenerator {

    private lateinit var classBuilder: TypeSpec.Builder

    fun createMigrationForTable(tableVersionMap: Map<Int, KotlinContainer.KotlinClass>) {
        val petalInfo: Petal = tableVersionMap[1]!!.getAnnotation(Petal::class)
            ?: printThenThrowError("All tables must start with version 1")
        val className = "TableMigrations\$${petalInfo.tableName}"

        val fileSpecBuilder = FileSpec.builder(
            packageName = tableVersionMap[1]!!.packageName,
            fileName = className
        )

        classBuilder = TypeSpec.classBuilder(className)

        tableVersionMap.toSortedMap().forEach {
            classBuilder.addTextSpec("migrateV${it.key}", buildClassSpec(it.value, it.key))
        }

        fileSpecBuilder.addType(classBuilder.build())
        fileSpecBuilder.build().writeTo(File(AnnotationParser.kaptKotlinGeneratedDir))
    }

    private fun buildClassSpec(kotlinClass: KotlinContainer.KotlinClass, version: Int): String {
        val petalAnnotation: Petal = kotlinClass.getAnnotation(Petal::class) as Petal

        var tableCreationSql = "CREATE TABLE ${petalAnnotation.tableName} ( "

        kotlinClass.kotlinProperties.forEach{
            tableCreationSql += when (it.typeName) {
                String::class.asTypeName() -> "${it.simpleName} TEXT, "
                Int::class.asTypeName() -> "${it.simpleName} INT, "
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
