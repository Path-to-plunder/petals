package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.casadetasha.kexp.petals.annotations.PetalSchemaMigration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import java.io.File
import java.util.*

class DaoGenerator(private val tableName: String, private val schema: PetalSchemaMigration) {

    companion object {
        const val EXPOSED_TABLE_PACKAGE = "org.jetbrains.exposed.sql.Table.Dual"

        private const val PACKAGE_NAME: String = "com.casadetasha.kexp.petals"
    }

    private lateinit var tableBuilder: TypeSpec.Builder
    private lateinit var entityBuilder: TypeSpec.Builder

    fun generateFile() {
        val fileSpecBuilder = FileSpec.builder(
            packageName = PACKAGE_NAME,
            fileName = "${tableName}Petals"
        )

        generateClasses()
        fileSpecBuilder.addType(tableBuilder.build())
            .build()
            .writeTo(File(AnnotationParser.kaptKotlinGeneratedDir))
    }

    fun generateClasses() {
        tableBuilder = TypeSpec.Companion.objectBuilder("${tableName}Table")
            .superclass(Table::class)
        entityBuilder = TypeSpec.Companion.classBuilder("${tableName}Entity")
            .superclass(
                Entity::class.asClassName()
                    .parameterizedBy(
                        Int::class.asClassName()
                    )
            )


        schema.columnMigrations.values
            .filter { !it.isId!! }
            .forEach { column ->
                addTableColumn(column)
                addEntityColumn(column)
            }
    }

    private fun addTableColumn(column: PetalColumn) {
        tableBuilder.addProperty(
            PropertySpec.builder(
                column.name,
                Column::class.asClassName()
                    .parameterizedBy(column.kotlinType)
            ).initializer(
                getColumnInitializationBlock(column)
            )
                .build()
        )
    }

    private fun getColumnInitializationBlock(column: PetalColumn): CodeBlock {
        if (column.dataType.startsWith("CHARACTER VARYING")) {
            val charLimit = column.dataType
                .removePrefix("CHARACTER VARYING(")
                .removeSuffix(")")
            return CodeBlock.builder()
                .add(
                    "%M(%S, %L)",
                    MemberName(EXPOSED_TABLE_PACKAGE, "varchar"),
                    column.name,
                    charLimit
                ).build()
        }

        return CodeBlock.builder()
            .add(
                "%M(%S)",
                getColumnCreationMemberName(column),
                column.name
            ).build()
    }

    private fun getColumnCreationMemberName(column: PetalColumn): Any? {
        val methodName = when (column.dataType) {
            "uuid" -> "uuid"
            "TEXT" -> "text"
            "INT" -> "integer"
            "BIGINT" -> "long"
            else -> printThenThrowError(
                "INTERNAL LIBRARY ERROR: unsupported column (${column.dataType})" +
                        " found while parsing Dao for table $tableName"
            )
        }

        return MemberName(EXPOSED_TABLE_PACKAGE, methodName)
    }

    private fun addEntityColumn(column: PetalColumn) {

    }

    private val PetalColumn.kotlinType: ClassName
        get() {
            if (dataType.startsWith("CHARACTER VARYING")) {
                return String::class.asClassName()
            }

            return when (val type = dataType) {
                "uuid" -> UUID::class
                "TEXT" -> String::class
                "INT" -> Int::class
                "BIGINT" -> Long::class
                else -> printThenThrowError(
                    "INTERNAL LIBRARY ERROR: unsupported datatype ($type) found while" +
                            " parsing Dao for table $tableName"
                )
            }.asClassName()
        }
}
