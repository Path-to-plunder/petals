package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration

import com.casadetasha.kexp.annotationparser.AnnotationParser.kaptKotlinGeneratedDir
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.petals.processor.model.ParsedPetal
import com.casadetasha.kexp.generationdsl.dsl.FileTemplate
import com.squareup.kotlinpoet.ClassName

internal class MigrationSqlClassGenerator(private val petal: ParsedPetal) {

    companion object {
        private const val PACKAGE_NAME: String = "com.casadetasha.kexp.petals.migration"
    }

    fun createMigrationForTable() {
        petal.schemas[1] ?: printThenThrowError("All tables must contain a version 1")
        val className = "TableMigrations\$${petal.tableName}"

        FileTemplate.generateFile(
            directory = kaptKotlinGeneratedDir,
            packageName = PACKAGE_NAME,
            fileName = className) {

            createDbMigrationClassTemplate(petal, ClassName(PACKAGE_NAME, className))
        }.writeToDisk()
    }
}
