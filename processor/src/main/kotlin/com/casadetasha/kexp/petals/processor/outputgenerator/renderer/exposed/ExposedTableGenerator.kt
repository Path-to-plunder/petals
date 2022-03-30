package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.processor.inputparser.LocalPetalColumn
import com.casadetasha.kexp.petals.processor.inputparser.ParsedPetalSchema
import com.casadetasha.kexp.petals.processor.inputparser.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.inputparser.PetalValueColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.toMemberName
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed.ExposedClassesFileGenerator.Companion.EXPOSED_TABLE_PACKAGE
import com.squareup.kotlinpoet.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.dao.id.UUIDTable

internal class ExposedTableGenerator(
    private val className: String,
    private val tableName: String,
    private val schema: ParsedPetalSchema,
) {

    private val tableClassName: String by lazy { "${className}Table" }

    private val tableBuilder: TypeSpec.Builder by lazy {
        TypeSpec.Companion.objectBuilder(tableClassName)
            .superclass(getTableSuperclass())
            .addSuperclassConstructorParameter(CodeBlock.of("name = %S", tableName))
    }

    fun generateClassSpec(): TypeSpec = tableBuilder.build()

    fun addTableColumn(column: LocalPetalColumn) {
        tableBuilder.addProperty(
            PropertySpec.builder(
                column.name,
                column.tablePropertyClassName
            ).initializer(
                getColumnInitializationBlock(column)
            )
                .build()
        )
    }

    private fun getColumnInitializationBlock(column: LocalPetalColumn): CodeBlock {
        return when (column) {
            is PetalReferenceColumn -> getReferenceColumnInitializationBlock(column)
            is PetalValueColumn -> getValueColumnInitializationBlock(column)
            else -> printThenThrowError("INTERNAL LIBRARY ERROR: only value and reference petal columns allowed. Found ${column.kotlinType} for column ${column.name}.")
        }
    }

    private fun getValueColumnInitializationBlock(column: LocalPetalColumn): CodeBlock {
        if (column.dataType.startsWith("CHARACTER VARYING")) {
            val charLimit = column.dataType
                .removePrefix("CHARACTER VARYING(")
                .removeSuffix(")")
            val builder = CodeBlock.builder()
                .add(
                    "%M(%S, %L)",
                    MemberName(EXPOSED_TABLE_PACKAGE, "varchar"),
                    column.name,
                    charLimit
                )

            if (column.isNullable) {
                builder.add(".%M()", MemberName(EXPOSED_TABLE_PACKAGE, "nullable"))
            }
            return builder.build()
        }

        val builder = CodeBlock.builder()
            .add(
                "%M(%S)",
                getColumnCreationMemberName(column),
                column.name
            )

        if (column.isNullable) {
            builder.add(".%M()", MemberName(EXPOSED_TABLE_PACKAGE, "nullable"))
        }

        return builder.build()
    }

    private fun getReferenceColumnInitializationBlock(column: PetalReferenceColumn): CodeBlock {
        val builder = CodeBlock.builder()
            .add(
                "%M(%S,·%M)",
                getReferenceColumnCreationMemberName(),
                column.name,
                column.referencingTableClassName.toMemberName(),
            ).apply {
                if (column.isNullable) { add(".nullable()") }
            }

        return builder.build()
    }

    private fun getReferenceColumnCreationMemberName(): MemberName {
        return MemberName(EXPOSED_TABLE_PACKAGE, "reference")
    }

    private fun getColumnCreationMemberName(column: LocalPetalColumn): MemberName {
        val methodName = when (column.dataType) {
            "uuid" -> "uuid"
            "TEXT" -> "text"
            "INT" -> "integer"
            "BIGINT" -> "long"
            else -> AnnotationParser.printThenThrowError(
                "INTERNAL LIBRARY ERROR: unsupported column (${column.dataType})" +
                        " found while parsing Dao for table $className"
            )
        }

        return MemberName(EXPOSED_TABLE_PACKAGE, methodName)
    }

    private fun getTableSuperclass(): ClassName {
        return when (schema.primaryKeyType) {
            PetalPrimaryKey.INT -> IntIdTable::class.asClassName()
            PetalPrimaryKey.LONG -> LongIdTable::class.asClassName()
            PetalPrimaryKey.UUID -> UUIDTable::class.asClassName()
        }
    }
}
