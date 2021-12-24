package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.annotationparser.KotlinContainer
import com.casadetasha.kexp.petals.annotations.Petal
import com.squareup.kotlinpoet.*
import java.io.File

object MigrationGenerator {

    fun createMigrationForPetal(petalClass: KotlinContainer.KotlinClass) {
        val fileSpec = FileSpec.builder(
            packageName = petalClass.packageName,
            fileName = "${petalClass.classSimpleName}Migration"
        )
            .addType(buildClassSpec(petalClass))
            .build()

        fileSpec.writeTo(File(AnnotationParser.kaptKotlinGeneratedDir))
    }

    private fun buildClassSpec(kotlinClass: KotlinContainer.KotlinClass): TypeSpec {
        val specBuilder = TypeSpec.classBuilder("${kotlinClass.classSimpleName}Migration")
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

        specBuilder.addTextSpec(tableCreationSql)

        return specBuilder.build()
    }
}

private fun TypeSpec.Builder.addTextSpec(migrationSql: String) {
    addFunction(FunSpec.builder("migrate")
        .returns(String::class)
        .addCode(
            CodeBlock.builder()
                .add("return %S", migrationSql)
                .build()
        )
        .build())
}
