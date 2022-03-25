package com.casadetasha.kexp.petals.processor.classgenerator.table

import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
import com.casadetasha.kexp.petals.processor.UnprocessedPetalColumn
import com.casadetasha.kexp.petals.processor.UnprocessedPetalSchemaMigration
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions.toMemberName
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.EntityID
import java.util.*

internal class ExposedEntityGenerator(private val className: String,
                             private val schema: UnprocessedPetalSchemaMigration
) {

    companion object {
        const val EXPOSED_TABLE_PACKAGE = "org.jetbrains.exposed.sql.Table.Dual"
        const val PACKAGE_NAME: String = "com.casadetasha.kexp.petals"
    }

    private val tableClassName: String by lazy { "${className}Table" }
    private val entityClassName: String by lazy { "${className}Entity" }

    private val entityBuilder: TypeSpec.Builder by lazy {
        TypeSpec.Companion.classBuilder(entityClassName)
            .primaryConstructor(primaryConstructor
            )
            .superclass(getEntitySuperclass())
            .addSuperclassConstructorParameter("id")
            .addType(entityType
            )
    }

    private val primaryConstructor: FunSpec by lazy {
        FunSpec.constructorBuilder()
            .addParameter("id",
                EntityID::class
                    .asClassName()
                    .parameterizedBy(getEntityIdParameter()))
            .build()
    }

    private val entityType: TypeSpec by lazy {
        TypeSpec
            .companionObjectBuilder()
            .superclass(
                getEntityClassName()
                    .parameterizedBy(ClassName(PACKAGE_NAME, entityClassName)))
            .addSuperclassConstructorParameter(tableClassName)
            .build()
    }

    fun generateClassSpec(): TypeSpec = entityBuilder.build()

    fun addEntityColumn(column: UnprocessedPetalColumn) {
        when (column.isReferenceColumn) {
            true -> entityBuilder
                .addProperty(
                    PropertySpec.builder(column.name, column.entityPropertyClassName)
                        .mutable()
                        .delegate(
                            CodeBlock.of(
                                "%M %L %L.%L",
                                column.referencingEntityClassName!!.toMemberName(),
                                "referencedOn",
                                tableClassName,
                                column.name,
                            )
                        ).build()
                )

            false -> entityBuilder
                    .addProperty(
                        PropertySpec.builder(column.name, column.entityPropertyClassName)
                            .mutable()
                            .delegate(
                                CodeBlock.of(
                                    "%M.%L", MemberName(PACKAGE_NAME, tableClassName), column.name)
                            ).build()
                    )
        }
    }

    private fun getEntityClassName(): ClassName {
        return when (schema.primaryKeyType) {
            PetalPrimaryKey.INT -> IntEntityClass::class.asClassName()
            PetalPrimaryKey.LONG -> LongEntityClass::class.asClassName()
            PetalPrimaryKey.UUID -> UUIDEntityClass::class.asClassName()
        }
    }

    private fun getEntitySuperclass(): ClassName {
        return when (schema.primaryKeyType) {
            PetalPrimaryKey.INT -> IntEntity::class.asClassName()
            PetalPrimaryKey.LONG -> LongEntity::class.asClassName()
            PetalPrimaryKey.UUID -> UUIDEntity::class.asClassName()
        }
    }

    private fun getEntityIdParameter(): ClassName {
        return when (schema.primaryKeyType) {
            PetalPrimaryKey.INT -> Int::class.asClassName()
            PetalPrimaryKey.LONG -> Long::class.asClassName()
            PetalPrimaryKey.UUID -> UUID::class.asClassName()
        }
    }
}
