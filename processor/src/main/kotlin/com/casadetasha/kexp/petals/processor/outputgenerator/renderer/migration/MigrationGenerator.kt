package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.petals.processor.model.ParsedPetal
import com.squareup.kotlinpoet.*
import java.io.File

internal class MigrationGenerator(private val petal: ParsedPetal) {

    companion object {
        private const val PACKAGE_NAME: String = "com.casadetasha.kexp.petals.migration"
    }

    fun createMigrationForTable() {
        petal.schemas[1] ?: printThenThrowError("All tables must contain a version 1")
        val className = "TableMigrations\$${petal.tableName}"

        FileSpec.builder(packageName = PACKAGE_NAME, fileName = className)
            .addType(MigrationClassSpecParser(petal, className).petalMigrationClassSpec)
            .build()
            .writeTo(File(AnnotationParser.kaptKotlinGeneratedDir))
    }
}
