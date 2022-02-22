package com.casadetasha.kexp.petals.processor.classgenerator.table

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.annotations.PetalSchemaMigration
import com.casadetasha.kexp.petals.processor.classgenerator.table.ExposedEntityGenerator.Companion.EXPOSED_TABLE_PACKAGE
import com.casadetasha.kexp.petals.processor.ktx.kotlinType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column

internal class ExposedTableGenerator(private val className: String,
                                     private val tableName: String,
                                     private val schema: PetalSchemaMigration
) {

    private val tableClassName: String by lazy { "${className}Table" }

    private val tableBuilder: TypeSpec.Builder by lazy {
        TypeSpec.Companion.objectBuilder(tableClassName)
            .superclass(getTableSuperclass())
            .addSuperclassConstructorParameter(CodeBlock.of("name = %S", tableName))
    }

    fun generateClassSpec(): TypeSpec = tableBuilder.build()

    fun addTableColumn(column: PetalColumn) {
        tableBuilder.addProperty(
            PropertySpec.builder(
                column.name,
                Column::class.asClassName()
                    .parameterizedBy(column.kotlinType.copy(nullable = column.isNullable))
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
            val builder = CodeBlock.builder()
                .add(
                    "%M(%S, %L)",
                    MemberName(ExposedGenerator.EXPOSED_TABLE_PACKAGE, "varchar"),
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

    private fun getColumnCreationMemberName(column: PetalColumn): MemberName {
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

        return MemberName(ExposedGenerator.EXPOSED_TABLE_PACKAGE, methodName)
    }

    private fun getTableSuperclass(): ClassName {
        return when (schema.primaryKeyType) {
            PetalPrimaryKey.INT -> IntIdTable::class.asClassName()
            PetalPrimaryKey.LONG -> LongIdTable::class.asClassName()
            PetalPrimaryKey.UUID -> UUIDTable::class.asClassName()
        }
    }
}
