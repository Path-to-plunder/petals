package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.petals.processor.model.PetalClasses
import com.casadetasha.kexp.petals.processor.model.UnprocessedPetalSchemaMigration
import com.squareup.kotlinpoet.*
import java.io.File

internal class ExposedClassesFileGenerator(
    private val petalClasses: PetalClasses,
    private val className: String,
    private val tableName: String,
    private val schema: UnprocessedPetalSchemaMigration
) {

    companion object {
        const val EXPOSED_TABLE_PACKAGE = "org.jetbrains.exposed.sql.Table.Dual"
        private const val PACKAGE_NAME: String = "com.casadetasha.kexp.petals"
    }

    private val tableGenerator: ExposedTableGenerator by lazy {
        ExposedTableGenerator(
            className = className,
            tableName = tableName,
            schema = schema
        )
    }

    private val entityGenerator: ExposedEntityGenerator by lazy {
        ExposedEntityGenerator(
            petalClasses = petalClasses,
            className = className,
            schema = schema
        )
    }

    fun generateFile() {
        addColumnsToGenerators()

        FileSpec.builder(
            packageName = PACKAGE_NAME,
            fileName = "${className}Petals"
        ).addType(tableGenerator.generateClassSpec())
            .addType(entityGenerator.generateClassSpec())
            .build()
            .writeTo(File(AnnotationParser.kaptKotlinGeneratedDir))
    }

    private fun addColumnsToGenerators() {
        schema.columnsAsList
            .filter { !it.isId }
            .forEach { column ->
                if (column.isLocalColumn) { tableGenerator.addTableColumn(column) }
                entityGenerator.addEntityColumn(column)
            }
    }
}
