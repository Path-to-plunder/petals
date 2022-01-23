package com.casadetasha.kexp.petals.processor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.casadetasha.kexp.petals.annotations.PetalSchemaMigration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import java.io.File
import java.util.*

class DaoGenerator(private val className: String, private val schema: PetalSchemaMigration) {

    companion object {
        const val EXPOSED_TABLE_PACKAGE = "org.jetbrains.exposed.sql.Table.Dual"

        private const val PACKAGE_NAME: String = "com.casadetasha.kexp.petals"
    }

    private val tableClassName: String by lazy { "${className}Table" }
    private val entityClassName: String by lazy { "${className}Entity" }

    private lateinit var tableBuilder: TypeSpec.Builder
    private lateinit var entityBuilder: TypeSpec.Builder

    fun generateFile() {
        val fileSpecBuilder = FileSpec.builder(
            packageName = PACKAGE_NAME,
            fileName = "${className}Petals"
        )

        generateClasses()
        fileSpecBuilder
            .addType(tableBuilder.build())
            .addType(entityBuilder.build())
            .build()
            .writeTo(File(AnnotationParser.kaptKotlinGeneratedDir))
    }

    private fun generateClasses() {
        createTableBuilder()
        createEntityBuilder()

        schema.columnMigrations.values
            .filter { !it.isId!! }
            .forEach { column ->
                addTableColumn(column)
                addEntityColumn(column)
            }
    }

    private fun createTableBuilder() {
        tableBuilder = TypeSpec.Companion.objectBuilder(tableClassName)
            .superclass(IntIdTable::class)
    }

    private fun createEntityBuilder() {
        entityBuilder = TypeSpec.Companion.classBuilder(entityClassName)
            .primaryConstructor(FunSpec.constructorBuilder()
                .addParameter("id",
                    EntityID::class
                        .asClassName()
                        .parameterizedBy(Int::class.asClassName()))
                .build()
            )
            .superclass(IntEntity::class.asClassName())
            .addSuperclassConstructorParameter("id")
            .addType(
                TypeSpec
                    .companionObjectBuilder()
                    .superclass(
                        IntEntityClass::class.asClassName()
                            .parameterizedBy(ClassName(PACKAGE_NAME, entityClassName)))
                    .addSuperclassConstructorParameter(tableClassName)
                    .build()
            )
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

    private fun getColumnCreationMemberName(column: PetalColumn): MemberName {
        val methodName = when (column.dataType) {
            "uuid" -> "uuid"
            "TEXT" -> "text"
            "INT" -> "integer"
            "BIGINT" -> "long"
            else -> printThenThrowError(
                "INTERNAL LIBRARY ERROR: unsupported column (${column.dataType})" +
                        " found while parsing Dao for table $className"
            )
        }

        return MemberName(EXPOSED_TABLE_PACKAGE, methodName)
    }

    private fun addEntityColumn(column: PetalColumn) {
        entityBuilder
            .addProperty(PropertySpec.builder(column.name, column.kotlinType)
                .delegate(
                    CodeBlock.of(
                        "%M.%L", MemberName(PACKAGE_NAME, tableClassName), column.name)
                ).build()
            )
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
                            " parsing Dao for table $className"
                )
            }.asClassName()
        }
}
