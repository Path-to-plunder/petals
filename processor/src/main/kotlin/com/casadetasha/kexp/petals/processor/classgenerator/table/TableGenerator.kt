package com.casadetasha.kexp.petals.processor.classgenerator.table

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.annotationparser.AnnotationParser.printThenThrowError
import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.annotations.PetalSchemaMigration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import java.io.File
import java.util.*

class TableGenerator(private val className: String,
                     private val tableName: String,
                     private val schema: PetalSchemaMigration) {

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
            .superclass(getTableSuperclass())
            .addSuperclassConstructorParameter(CodeBlock.of("name = %S", tableName))
    }

    private fun getTableSuperclass(): ClassName {
        return when (schema.primaryKeyType) {
            PetalPrimaryKey.INT -> IntIdTable::class.asClassName()
            PetalPrimaryKey.LONG -> LongIdTable::class.asClassName()
            PetalPrimaryKey.UUID -> UUIDTable::class.asClassName()
            PetalPrimaryKey.NONE -> IntIdTable::class.asClassName()
        }
    }

    private fun createEntityBuilder() {
        entityBuilder = TypeSpec.Companion.classBuilder(entityClassName)
            .primaryConstructor(FunSpec.constructorBuilder()
                .addParameter("id",
                    EntityID::class
                        .asClassName()
                        .parameterizedBy(getEntityIdParameter()))
                .build()
            )
            .superclass(getEntitySuperclass())
            .addSuperclassConstructorParameter("id")
            .addType(
                TypeSpec
                    .companionObjectBuilder()
                    .superclass(
                        getEntityClassName()
                            .parameterizedBy(ClassName(PACKAGE_NAME, entityClassName)))
                    .addSuperclassConstructorParameter(tableClassName)
                    .build()
            )
    }

    private fun getEntityClassName(): ClassName {
        return when (schema.primaryKeyType) {
            PetalPrimaryKey.INT -> IntEntityClass::class.asClassName()
            PetalPrimaryKey.LONG -> LongEntityClass::class.asClassName()
            PetalPrimaryKey.UUID -> UUIDEntityClass::class.asClassName()
            PetalPrimaryKey.NONE -> IntEntityClass::class.asClassName()
        }
    }

    private fun getEntitySuperclass(): ClassName {
        return when (schema.primaryKeyType) {
            PetalPrimaryKey.INT -> IntEntity::class.asClassName()
            PetalPrimaryKey.LONG -> LongEntity::class.asClassName()
            PetalPrimaryKey.UUID -> UUIDEntity::class.asClassName()
            PetalPrimaryKey.NONE -> IntEntity::class.asClassName()
        }
    }

    private fun getEntityIdParameter(): ClassName {
        return when (schema.primaryKeyType) {
            PetalPrimaryKey.INT -> Int::class.asClassName()
            PetalPrimaryKey.LONG -> Long::class.asClassName()
            PetalPrimaryKey.UUID -> UUID::class.asClassName()
            PetalPrimaryKey.NONE -> Int::class.asClassName()
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
                .mutable()
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
