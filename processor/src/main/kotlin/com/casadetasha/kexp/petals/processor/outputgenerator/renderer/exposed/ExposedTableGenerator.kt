package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.processor.model.UnprocessedPetalColumn
import com.casadetasha.kexp.petals.processor.model.UnprocessedPetalSchemaMigration
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.toMemberName
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed.ExposedClassesFileGenerator.Companion.EXPOSED_TABLE_PACKAGE
import com.squareup.kotlinpoet.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.dao.id.UUIDTable

internal class ExposedTableGenerator(
    private val className: String,
    private val tableName: String,
    private val schema: UnprocessedPetalSchemaMigration,
) {

    private val tableClassName: String by lazy { "${className}Table" }

    private val tableBuilder: TypeSpec.Builder by lazy {
        TypeSpec.Companion.objectBuilder(tableClassName)
            .superclass(getTableSuperclass())
            .addSuperclassConstructorParameter(CodeBlock.of("name = %S", tableName))
    }

    fun generateClassSpec(): TypeSpec = tableBuilder.build()

    fun addTableColumn(column: UnprocessedPetalColumn) {
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

    private fun getColumnInitializationBlock(column: UnprocessedPetalColumn): CodeBlock {
        return when (column.isReferenceColumn) {
            true -> getReferenceColumnInitializationBlock(column)
            false -> getValueColumnInitializationBlock(column)
        }
    }

    private fun getValueColumnInitializationBlock(column: UnprocessedPetalColumn): CodeBlock {
        if (column.dataType.startsWith("CHARACTER VARYING")) {
            val charLimit = column.dataType
                .removePrefix("CHARACTER VARYING(")
                .removeSuffix(")")
            val builder = CodeBlock.builder()
                .add(
                    "%M(%S, %L)",
                    MemberName(ExposedClassesFileGenerator.EXPOSED_TABLE_PACKAGE, "varchar"),
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

    private fun getReferenceColumnInitializationBlock(column: UnprocessedPetalColumn): CodeBlock {
        val builder = CodeBlock.builder()
            .add(
                "%M(%S,·%M)",
                getReferenceColumnCreationMemberName(),
                column.name,
                column.referencingTableClassName!!.toMemberName(),
            ).apply {
                if (column.isNullable) { add(".nullable()") }
            }

        return builder.build()
    }

    private fun getReferenceColumnCreationMemberName(): MemberName {
        return MemberName(ExposedClassesFileGenerator.EXPOSED_TABLE_PACKAGE, "reference")
    }

    private fun getColumnCreationMemberName(column: UnprocessedPetalColumn): MemberName {
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

        return MemberName(ExposedClassesFileGenerator.EXPOSED_TABLE_PACKAGE, methodName)
    }

    private fun getTableSuperclass(): ClassName {
        return when (schema.primaryKeyType) {
            PetalPrimaryKey.INT -> IntIdTable::class.asClassName()
            PetalPrimaryKey.LONG -> LongIdTable::class.asClassName()
            PetalPrimaryKey.UUID -> UUIDTable::class.asClassName()
        }
    }
}
